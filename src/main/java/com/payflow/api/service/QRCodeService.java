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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QRCodeService {

    private final QRCodeRepository qrCodeRepository;
    private final WalletService walletService;
    private final TransactionService transactionService;

    /**
     * Create a QR code for a wallet
     */
    @Transactional
    public QRCode createWalletQRCode(User user, String walletNumber, BigDecimal amount,
            boolean isAmountFixed, boolean isOneTime,
            String description, LocalDateTime expiresAt) {
        // Get the wallet
        Wallet wallet = walletService.getWalletByNumber(walletNumber);

        // Ensure the wallet belongs to the user
        if (!wallet.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only create QR codes for your own wallets");
        }

        // Create the QR code
        QRCode qrCode = new QRCode(wallet, amount, isAmountFixed, isOneTime, description, expiresAt);

        return qrCodeRepository.save(qrCode);
    }

    /**
     * Get a QR code by ID
     */
    public QRCode getQRCodeById(Long id) {
        return qrCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QRCode", "id", id));
    }

    /**
     * Get a QR code by QR ID
     */
    public QRCode getQRCodeByQrId(String qrId) {
        return qrCodeRepository.findByQrId(qrId)
                .orElseThrow(() -> new ResourceNotFoundException("QRCode", "qrId", qrId));
    }

    /**
     * Get all QR codes for a user
     */
    public List<QRCode> getUserQRCodes(User user) {
        return qrCodeRepository.findByUser(user);
    }

    /**
     * Get all QR codes for a wallet
     */
    public List<QRCode> getWalletQRCodes(Wallet wallet) {
        return qrCodeRepository.findByWallet(wallet);
    }

    /**
     * Process a QR code payment
     */
    @Transactional
    public Transaction processQRCodePayment(User sender, String qrId, BigDecimal amount, String sourceWalletNumber) {
        // Get the QR code
        QRCode qrCode = getQRCodeByQrId(qrId);

        // Ensure the QR code is active
        if (!qrCode.isActive()) {
            throw new BadRequestException("This QR code is no longer active");
        }

        // Check expiration
        if (qrCode.getExpiresAt() != null && qrCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This QR code has expired");
        }

        // Get the source wallet
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

        // Get destination wallet
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
     */
    public String generateQRCodeImage(String qrId) {
        QRCode qrCode = getQRCodeByQrId(qrId);
        String qrContent = createQRCodeContent(qrCode);

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] qrCodeBytes = outputStream.toByteArray();

            return Base64.getEncoder().encodeToString(qrCodeBytes);
        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR code image", e);
            throw new BadRequestException("Failed to generate QR code image");
        }
    }

    /**
     * Create QR code content
     */
    private String createQRCodeContent(QRCode qrCode) {
        StringBuilder content = new StringBuilder();
        content.append("payflow://payment?");
        content.append("qr_id=").append(qrCode.getQrId());
        content.append("&wallet=").append(qrCode.getWallet().getWalletNumber());
        content.append("&currency=").append(qrCode.getWallet().getCurrency());

        if (qrCode.isAmountFixed() && qrCode.getAmount() != null) {
            content.append("&amount=").append(qrCode.getAmount());
        }

        if (qrCode.getDescription() != null) {
            content.append("&description=").append(qrCode.getDescription());
        }

        return content.toString();
    }

    /**
     * Deactivate a QR code
     */
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
}
