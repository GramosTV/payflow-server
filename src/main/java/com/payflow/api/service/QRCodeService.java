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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing QR codes and QR code-based payments.
 *
 * <p>This service handles the creation, retrieval, and processing of QR codes for wallet-to-wallet
 * payments. It also provides functionality for generating QR code images and managing QR code
 * lifecycle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QRCodeService {

  private final QRCodeRepository qrCodeRepository;
  private final WalletService walletService;
  private final TransactionService transactionService;

  /**
   * Creates a new QR code for a wallet with specified parameters.
   *
   * @param user the user creating the QR code
   * @param walletNumber the wallet number for which to create the QR code
   * @param amount the payment amount (can be null if not fixed)
   * @param isAmountFixed whether the amount is fixed and cannot be changed
   * @param isOneTime whether this is a one-time use QR code
   * @param description optional description for the QR code
   * @param expiresAt optional expiration date/time
   * @return the created QR code
   * @throws BadRequestException if the wallet doesn't belong to the user
   */
  @Transactional
  public QRCode createWalletQRCode(
      final User user,
      final String walletNumber,
      final BigDecimal amount,
      final boolean isAmountFixed,
      final boolean isOneTime,
      final String description,
      final LocalDateTime expiresAt) {
    final Wallet wallet = walletService.getWalletByNumber(walletNumber);

    // Ensure the wallet belongs to the user
    if (!wallet.getUser().getId().equals(user.getId())) {
      throw new BadRequestException("You can only create QR codes for your own wallets");
    } // Create the QR code
    final QRCode qrCode =
        new QRCode(wallet, amount, isAmountFixed, isOneTime, description, expiresAt);

    return qrCodeRepository.save(qrCode);
  }

  /**
   * Retrieves a QR code by its ID.
   *
   * @param id the QR code ID
   * @return the QR code
   * @throws ResourceNotFoundException if no QR code found with the given ID
   */
  public QRCode getQRCodeById(final Long id) {
    return qrCodeRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("QRCode", "id", id));
  }

  /**
   * Retrieves a QR code by its unique QR ID.
   *
   * @param qrId the unique QR ID
   * @return the QR code
   * @throws ResourceNotFoundException if no QR code found with the given QR ID
   */
  public QRCode getQRCodeByQrId(final String qrId) {
    return qrCodeRepository
        .findByQrId(qrId)
        .orElseThrow(() -> new ResourceNotFoundException("QRCode", "qrId", qrId));
  }

  /**
   * Retrieves all QR codes belonging to a specific user.
   *
   * @param user the user whose QR codes to retrieve
   * @return list of QR codes belonging to the user
   */
  public List<QRCode> getUserQRCodes(final User user) {
    return qrCodeRepository.findByUser(user);
  }

  /**
   * Retrieves all QR codes for a specific wallet.
   *
   * @param wallet the wallet whose QR codes to retrieve
   * @return list of QR codes for the wallet
   */
  public List<QRCode> getWalletQRCodes(final Wallet wallet) {
    return qrCodeRepository.findByWallet(wallet);
  }

  /**
   * Processes a QR code payment transaction.
   *
   * @param sender the user making the payment
   * @param qrId the QR code ID to process
   * @param amount the payment amount (ignored if QR code has fixed amount)
   * @param sourceWalletNumber the wallet number to send payment from
   * @return the completed transaction
   * @throws BadRequestException if QR code is invalid or payment cannot be processed
   */
  @Transactional
  public Transaction processQRCodePayment(
      final User sender,
      final String qrId,
      final BigDecimal amount,
      final String sourceWalletNumber) {
    final QRCode qrCode = getQRCodeByQrId(qrId);

    // Ensure the QR code is active
    if (!qrCode.isActive()) {
      throw new BadRequestException("This QR code is no longer active");
    }

    // Check expiration
    if (qrCode.getExpiresAt() != null && qrCode.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BadRequestException("This QR code has expired");
    }

    final Wallet sourceWallet = walletService.getWalletByNumber(sourceWalletNumber);

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

    final Wallet destinationWallet = qrCode.getWallet();

    // Create transaction
    final Transaction transaction = new Transaction();
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

    final Transaction processedTransaction =
        transactionService.processQRCodeTransaction(transaction);

    // If it's a one-time QR code, deactivate it
    if (qrCode.isOneTime()) {
      qrCode.setActive(false);
      qrCodeRepository.save(qrCode);
    }

    return processedTransaction;
  }

  /**
   * Generates QR code image as base64 string.
   *
   * <p>This method is annotated with @Transactional to avoid LazyInitializationException when
   * accessing wallet outside of a transaction. The readOnly=true flag optimizes database access for
   * better performance.
   *
   * <p>In a production environment, consider adding caching to this method:
   *
   * <ul>
   *   <li>Add a @Cacheable annotation with a TTL policy
   *   <li>Implement a distributed cache like Redis for clustered environments
   *   <li>Use a file-based cache for QR images to reduce CPU usage
   * </ul>
   *
   * @param qrId the QR ID to generate image for
   * @return base64 encoded QR code image
   * @throws ResourceNotFoundException if QR code not found
   * @throws BadRequestException if image generation fails
   */
  @Transactional(readOnly = true)
  public String generateQRCodeImage(final String qrId) {
    log.info("Generating QR code image for qrId: {}", qrId);
    try {
      final QRCode qrCode =
          qrCodeRepository
              .findByQrIdWithWallet(qrId)
              .orElseThrow(
                  () -> {
                    log.warn("QR code not found with qrId: {}", qrId);
                    return new ResourceNotFoundException("QRCode", "qrId", qrId);
                  });

      // Force initialization of the wallet entity to avoid
      // LazyInitializationException
      if (qrCode.getWallet() != null) {
        final String walletNumber = qrCode.getWallet().getWalletNumber(); // Force initialization
        log.debug("Successfully initialized wallet {} for QR code: {}", walletNumber, qrId);
      } else {
        log.warn("Wallet is null for QR code: {}", qrId);
      }

      // Create the content for the QR code
      final String qrContent = createQRCodeContent(qrCode);
      log.debug("Generated QR content for qrId: {}", qrId);

      // Generate the QR code
      final QRCodeWriter qrCodeWriter = new QRCodeWriter();
      final BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 300, 300);

      // Convert to an image
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
      final byte[] qrCodeBytes = outputStream.toByteArray();

      // Return as base64 string
      return Base64.getEncoder().encodeToString(qrCodeBytes);
    } catch (final ResourceNotFoundException e) {
      log.error("QR code not found: {}", qrId, e);
      throw e;
    } catch (final WriterException e) {
      log.error("Error encoding QR code: {}", qrId, e);
      throw new BadRequestException("Failed to generate QR code image: " + e.getMessage());
    } catch (final IOException e) {
      log.error("I/O error generating QR code image: {}", qrId, e);
      throw new BadRequestException("Failed to generate QR code image: " + e.getMessage());
    } catch (final Exception e) {
      log.error("Unexpected error generating QR code image: {}", qrId, e);
      throw new BadRequestException("Failed to generate QR code image: " + e.getMessage());
    }
  }

  /**
   * Creates QR code content with proper URL encoding.
   *
   * @param qrCode the QR code entity
   * @return formatted QR code content string
   * @throws BadRequestException if content creation fails
   */
  private String createQRCodeContent(final QRCode qrCode) {
    try {
      final StringBuilder content = new StringBuilder();
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
        final String encodedDescription =
            java.net.URLEncoder.encode(qrCode.getDescription(), "UTF-8");
        content.append("&description=").append(encodedDescription);
      }

      return content.toString();
    } catch (final Exception e) {
      log.error("Error creating QR code content for QR ID: {}", qrCode.getQrId(), e);
      throw new BadRequestException("Failed to create QR code content: " + e.getMessage());
    }
  }

  /**
   * Deactivates a QR code, making it unusable for payments.
   *
   * @param user the user requesting deactivation
   * @param qrId the QR ID to deactivate
   * @throws BadRequestException if the QR code doesn't belong to the user
   * @throws ResourceNotFoundException if QR code not found
   */
  @Transactional
  public void deactivateQRCode(final User user, final String qrId) {
    final QRCode qrCode = getQRCodeByQrId(qrId);

    // Ensure the QR code belongs to the user
    if (!qrCode.getWallet().getUser().getId().equals(user.getId())) {
      throw new BadRequestException("You can only deactivate your own QR codes");
    }

    qrCode.setActive(false);
    qrCodeRepository.save(qrCode);
  }

  /**
   * Gets a QR code by ID with Wallet eagerly loaded. This prevents LazyInitializationException when
   * accessing wallet outside transaction.
   *
   * @param id the QR code ID
   * @return QR code with wallet eagerly loaded
   * @throws ResourceNotFoundException if QR code not found
   */
  @Transactional(readOnly = true)
  public QRCode getQRCodeByIdWithWallet(final Long id) {
    final QRCode qrCode = getQRCodeById(id);

    // Reload the QR code with the wallet eagerly fetched if we have the QR ID
    if (qrCode != null && qrCode.getQrId() != null) {
      // Try to use the repository method that eagerly fetches the wallet
      return qrCodeRepository
          .findByQrIdWithWallet(qrCode.getQrId())
          .orElse(initializeQRCodeWallet(qrCode));
    }

    return initializeQRCodeWallet(qrCode);
  }

  /**
   * Initializes the wallet entity to avoid LazyInitializationException.
   *
   * @param qrCode the QR code to initialize
   * @return the initialized QR code
   */
  private QRCode initializeQRCodeWallet(final QRCode qrCode) {
    if (qrCode != null && qrCode.getWallet() != null) {
      // Force initialization of wallet and user to avoid LazyInitializationException
      final String walletNumber = qrCode.getWallet().getWalletNumber();
      log.debug("Initialized wallet: {}", walletNumber);
      if (qrCode.getWallet().getUser() != null) {
        final Long userId = qrCode.getWallet().getUser().getId();
        log.debug("Initialized user: {}", userId);
      }
    }
    return qrCode;
  }
}
