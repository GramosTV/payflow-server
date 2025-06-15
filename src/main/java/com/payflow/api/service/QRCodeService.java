package com.payflow.api.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.payflow.api.exception.BadRequestException;
import com.payflow.api.exception.ResourceNotFoundException;
import com.payflow.api.model.entity.QRCode;
import com.payflow.api.model.entity.Transaction;
import com.payflow.api.model.entity.User;
import com.payflow.api.model.entity.Wallet;
import com.payflow.api.repository.QRCodeRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class QRCodeService {

  private final QRCodeRepository qrCodeRepository;
  private final WalletService walletService;
  private final TransactionService transactionService;

  public QRCodeService(
      QRCodeRepository qrCodeRepository,
      WalletService walletService,
      TransactionService transactionService) {
    this.qrCodeRepository = qrCodeRepository;
    this.walletService = walletService;
    this.transactionService = transactionService;
  }

  @Transactional
  public QRCode createWalletQRCode(
      User user,
      String walletNumber,
      BigDecimal amount,
      boolean isAmountFixed,
      boolean isOneTime,
      String description,
      LocalDateTime expiresAt) {
    Wallet wallet = walletService.getWalletByNumber(walletNumber);

    // Ensure the wallet belongs to the user
    if (!wallet.getUser().getId().equals(user.getId())) {
      throw new BadRequestException("You can only create QR codes for your own wallets");
    }

    // Create the QR code
    QRCode qrCode = new QRCode(wallet, amount, isAmountFixed, isOneTime, description, expiresAt);

    return qrCodeRepository.save(qrCode);
  }

  public QRCode getQRCodeById(Long id) {
    return qrCodeRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("QRCode", "id", id));
  }

  public QRCode getQRCodeByQrId(String qrId) {
    return qrCodeRepository
        .findByQrId(qrId)
        .orElseThrow(() -> new ResourceNotFoundException("QRCode", "qrId", qrId));
  }

  public List<QRCode> getUserQRCodes(User user) {
    return qrCodeRepository.findByUser(user);
  }

  public List<QRCode> getWalletQRCodes(Wallet wallet) {
    return qrCodeRepository.findByWallet(wallet);
  }

  /** Process a QR code payment */
  @Transactional
  public Transaction processQRCodePayment(
      User sender, String qrId, BigDecimal amount, String sourceWalletNumber) {
    QRCode qrCode = getQRCodeByQrId(qrId);

    // Ensure the QR code is active
    if (!qrCode.isActive()) {
      throw new BadRequestException("This QR code is no longer active");
    }

    // Check expiration
    if (qrCode.getExpiresAt() != null && qrCode.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BadRequestException("This QR code has expired");
    }
    Wallet sourceWallet = walletService.getWalletByNumber(sourceWalletNumber);

    // Ensure the source wallet belongs to the sender
    if (!sourceWallet.getUser().getId().equals(sender.getId())) {
      throw new BadRequestException("You can only pay with your own wallet");
    }

    // Check if amount is fixed
    BigDecimal paymentAmount = amount;
    if (qrCode.isAmountFixed()) {
      paymentAmount = qrCode.getAmount();
    }

    // Ensure the amount is positive
    if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BadRequestException("Payment amount must be positive");
    }

    // Check if the source wallet has enough balance
    if (sourceWallet.getBalance().compareTo(paymentAmount) < 0) {
      throw new BadRequestException("Insufficient balance in source wallet");
    }
    Wallet destinationWallet = qrCode.getWallet();

    // Create transaction
    Transaction transaction = new Transaction();
    transaction.setSender(sender);
    transaction.setReceiver(destinationWallet.getUser());
    transaction.setSourceWallet(sourceWallet);
    transaction.setDestinationWallet(destinationWallet);
    transaction.setAmount(paymentAmount);
    transaction.setSourceCurrency(sourceWallet.getCurrency());
    transaction.setDestinationCurrency(destinationWallet.getCurrency());
    transaction.setType(Transaction.TransactionType.TRANSFER);
    transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
    transaction.setDescription("QR Code payment: " + qrCode.getDescription());
    transaction.setQrCodeId(qrCode.getQrId());

    transaction = transactionService.processQRCodeTransaction(transaction);

    // If it's a one-time QR code, deactivate it
    if (qrCode.isOneTime()) {
      qrCode.setActive(false);
      qrCodeRepository.save(qrCode);
    }

    return transaction;
  }

  /**
   * Generate QR code image as base64 string
   *
   * <p>This method is annotated with @Transactional to avoid LazyInitializationException when
   * accessing wallet outside of a transaction. The readOnly=true flag optimizes database access for
   * better performance.
   *
   * <p>In a production environment, consider adding caching to this method: 1. Add a @Cacheable
   * annotation with a TTL policy 2. Implement a distributed cache like Redis for clustered
   * environments 3. Use a file-based cache for QR images to reduce CPU usage
   */
  @Transactional(readOnly = true)
  public String generateQRCodeImage(String qrId) {
    log.info("Generating QR code image for qrId: {}", qrId);
    try {
      QRCode qrCode =
          qrCodeRepository
              .findByQrIdWithWallet(qrId)
              .orElseThrow(
                  () -> {
                    log.warn("QR code not found with qrId: {}", qrId);
                    return new ResourceNotFoundException("QRCode", "qrId", qrId);
                  }); // Force initialization of the wallet entity to avoid
      // LazyInitializationException
      if (qrCode.getWallet() != null) {
        String walletNumber = qrCode.getWallet().getWalletNumber(); // Force initialization
        log.debug("Successfully initialized wallet {} for QR code: {}", walletNumber, qrId);
      } else {
        log.warn("Wallet is null for QR code: {}", qrId);
      }

      // Create the content for the QR code
      String qrContent = createQRCodeContent(qrCode);
      log.debug("Generated QR content for qrId: {}", qrId);

      // Generate the QR code
      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 300, 300);

      // Convert to an image
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
      byte[] qrCodeBytes = outputStream.toByteArray();

      // Return as base64 string
      return Base64.getEncoder().encodeToString(qrCodeBytes);
    } catch (ResourceNotFoundException e) {
      log.error("QR code not found: {}", qrId, e);
      throw e;
    } catch (WriterException e) {
      log.error("Error encoding QR code: {}", qrId, e);
      throw new BadRequestException("Failed to generate QR code image: " + e.getMessage());
    } catch (IOException e) {
      log.error("I/O error generating QR code image: {}", qrId, e);
      throw new BadRequestException("Failed to generate QR code image: " + e.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error generating QR code image: {}", qrId, e);
      throw new BadRequestException("Failed to generate QR code image: " + e.getMessage());
    }
  }

  /** Create QR code content with proper URL encoding */
  private String createQRCodeContent(QRCode qrCode) {
    try {
      StringBuilder content = new StringBuilder();
      content.append("payflow://payment?");
      content.append("qr_id=").append(qrCode.getQrId());

      // Check if wallet is not null before accessing
      if (qrCode.getWallet() != null) {
        content.append("&wallet=").append(qrCode.getWallet().getWalletNumber());
        content.append("&currency=").append(qrCode.getWallet().getCurrency());
      } else {
        log.warn("QR code {} has null wallet", qrCode.getQrId());
        throw new BadRequestException("Invalid QR code: missing wallet information");
      }

      if (qrCode.isAmountFixed() && qrCode.getAmount() != null) {
        content.append("&amount=").append(qrCode.getAmount());
      }

      if (qrCode.getDescription() != null && !qrCode.getDescription().isEmpty()) {
        // URL encode the description to handle special characters
        String encodedDescription = java.net.URLEncoder.encode(qrCode.getDescription(), "UTF-8");
        content.append("&description=").append(encodedDescription);
      }

      return content.toString();
    } catch (Exception e) {
      log.error("Error creating QR code content for QR ID: {}", qrCode.getQrId(), e);
      throw new BadRequestException("Failed to create QR code content: " + e.getMessage());
    }
  }

  @Transactional
  public void deactivateQRCode(User user, String qrId) {
    QRCode qrCode = getQRCodeByQrId(qrId);

    // Ensure the QR code belongs to the user
    if (!qrCode.getWallet().getUser().getId().equals(user.getId())) {
      throw new BadRequestException("You can only deactivate your own QR codes");
    }

    qrCode.setActive(false);
    qrCodeRepository.save(qrCode);
  }

  /**
   * Get a QR code by ID with Wallet eagerly loaded This prevents LazyInitializationException when
   * accessing wallet outside transaction
   */
  @Transactional(readOnly = true)
  public QRCode getQRCodeByIdWithWallet(Long id) {
    QRCode qrCode = getQRCodeById(id);

    // Reload the QR code with the wallet eagerly fetched if we have the QR ID
    if (qrCode != null && qrCode.getQrId() != null) {
      // Try to use the repository method that eagerly fetches the wallet
      return qrCodeRepository
          .findByQrIdWithWallet(qrCode.getQrId())
          .orElse(initializeQRCodeWallet(qrCode));
    }

    return initializeQRCodeWallet(qrCode);
  }

  /** Initialize the wallet entity to avoid LazyInitializationException */
  private QRCode initializeQRCodeWallet(QRCode qrCode) {
    if (qrCode != null && qrCode.getWallet() != null) {
      // Force initialization of wallet and user to avoid LazyInitializationException
      String walletNumber = qrCode.getWallet().getWalletNumber();
      log.debug("Initialized wallet: {}", walletNumber);
      if (qrCode.getWallet().getUser() != null) {
        Long userId = qrCode.getWallet().getUser().getId();
        log.debug("Initialized user: {}", userId);
      }
    }
    return qrCode;
  }
}
