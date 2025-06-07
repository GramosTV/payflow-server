package com.payflow.api.service;

import com.payflow.api.exception.BadRequestException;
import com.payflow.api.exception.ResourceNotFoundException;
import com.payflow.api.model.entity.QRCode;
import com.payflow.api.model.entity.Transaction;
import com.payflow.api.model.entity.User;
import com.payflow.api.model.entity.Wallet;
import com.payflow.api.repository.QRCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class QRCodeServiceTest {

    @Mock
    private QRCodeRepository qrCodeRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private QRCodeService qrCodeService;

    private User testUser;
    private User otherUser;
    private Wallet userWallet;
    private Wallet otherWallet;
    private QRCode testQRCode;
    private Transaction testTransaction;
    private String qrId = "TEST-QR-123456";

    @BeforeEach
    public void setup() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");
        otherUser.setFullName("Other User");

        userWallet = new Wallet();
        userWallet.setId(1L);
        userWallet.setUser(testUser);
        userWallet.setCurrency(Wallet.Currency.USD);
        userWallet.setBalance(BigDecimal.valueOf(1000));
        userWallet.setWalletNumber("WALLET123456");

        otherWallet = new Wallet();
        otherWallet.setId(2L);
        otherWallet.setUser(otherUser);
        otherWallet.setCurrency(Wallet.Currency.USD);
        otherWallet.setBalance(BigDecimal.valueOf(500));
        otherWallet.setWalletNumber("WALLET654321");

        testQRCode = new QRCode();
        testQRCode.setId(1L);
        testQRCode.setQrId(qrId);
        testQRCode.setWallet(userWallet);
        testQRCode.setAmount(BigDecimal.valueOf(50));
        testQRCode.setAmountFixed(true);
        testQRCode.setOneTime(false);
        testQRCode.setDescription("Test QR Code");
        testQRCode.setActive(true);
        testQRCode.setCreatedAt(LocalDateTime.now());
        testQRCode.setExpiresAt(LocalDateTime.now().plusDays(7));

        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setTransactionNumber("TXN123456");
        testTransaction.setSender(otherUser);
        testTransaction.setReceiver(testUser);
        testTransaction.setSourceWallet(otherWallet);
        testTransaction.setDestinationWallet(userWallet);
        testTransaction.setAmount(BigDecimal.valueOf(50));
        testTransaction.setType(Transaction.TransactionType.TRANSFER);
        testTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        testTransaction.setSourceCurrency(Wallet.Currency.USD);
        testTransaction.setDestinationCurrency(Wallet.Currency.USD);
        testTransaction.setQrCodeId(qrId);
    }

    @Test
    public void testCreateWalletQRCode_Success() {
        // Arrange
        when(walletService.getWalletByNumber(anyString())).thenReturn(userWallet);
        when(qrCodeRepository.save(any(QRCode.class))).thenReturn(testQRCode);

        BigDecimal amount = BigDecimal.valueOf(50);
        boolean isAmountFixed = true;
        boolean isOneTime = false;
        String description = "Test QR Code";
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        // Act
        QRCode result = qrCodeService.createWalletQRCode(
                testUser, userWallet.getWalletNumber(), amount, isAmountFixed, isOneTime, description, expiresAt);

        // Assert
        assertNotNull(result);
        assertEquals(testQRCode.getQrId(), result.getQrId());
        assertEquals(userWallet, result.getWallet());
        assertEquals(amount, result.getAmount());
        assertEquals(isAmountFixed, result.isAmountFixed());
        assertEquals(isOneTime, result.isOneTime());
        assertEquals(description, result.getDescription());
        assertEquals(expiresAt, result.getExpiresAt());

        // Verify
        verify(walletService).getWalletByNumber(userWallet.getWalletNumber());
        verify(qrCodeRepository).save(any(QRCode.class));
    }

    @Test
    public void testCreateWalletQRCode_NotOwner() {
        // Arrange
        when(walletService.getWalletByNumber(anyString())).thenReturn(otherWallet);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> qrCodeService.createWalletQRCode(
                testUser, otherWallet.getWalletNumber(), BigDecimal.valueOf(50),
                true, false, "Test", LocalDateTime.now().plusDays(7)));

        // Verify
        verify(walletService).getWalletByNumber(otherWallet.getWalletNumber());
        verify(qrCodeRepository, never()).save(any(QRCode.class));
    }

    @Test
    public void testGetQRCodeById_Success() {
        // Arrange
        when(qrCodeRepository.findById(anyLong())).thenReturn(Optional.of(testQRCode));

        // Act
        QRCode result = qrCodeService.getQRCodeById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testQRCode.getId(), result.getId());

        // Verify
        verify(qrCodeRepository).findById(1L);
    }

    @Test
    public void testGetQRCodeById_NotFound() {
        // Arrange
        when(qrCodeRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> qrCodeService.getQRCodeById(999L));

        // Verify
        verify(qrCodeRepository).findById(999L);
    }

    @Test
    public void testGetQRCodeByQrId_Success() {
        // Arrange
        when(qrCodeRepository.findByQrId(anyString())).thenReturn(Optional.of(testQRCode));

        // Act
        QRCode result = qrCodeService.getQRCodeByQrId(qrId);

        // Assert
        assertNotNull(result);
        assertEquals(testQRCode.getQrId(), result.getQrId());

        // Verify
        verify(qrCodeRepository).findByQrId(qrId);
    }

    @Test
    public void testGetUserQRCodes() {
        // Arrange
        List<QRCode> qrCodes = Arrays.asList(testQRCode);
        when(qrCodeRepository.findByUser(any(User.class))).thenReturn(qrCodes);

        // Act
        List<QRCode> result = qrCodeService.getUserQRCodes(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testQRCode.getId(), result.get(0).getId());

        // Verify
        verify(qrCodeRepository).findByUser(testUser);
    }

    @Test
    public void testProcessQRCodePayment_Success() {
        // Arrange
        when(qrCodeRepository.findByQrId(anyString())).thenReturn(Optional.of(testQRCode));
        when(walletService.getWalletByNumber(anyString())).thenReturn(otherWallet);
        when(transactionService.processQRCodeTransaction(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        Transaction result = qrCodeService.processQRCodePayment(
                otherUser, qrId, BigDecimal.valueOf(50), otherWallet.getWalletNumber());

        // Assert
        assertNotNull(result);
        assertEquals(testTransaction.getId(), result.getId());
        assertEquals(qrId, result.getQrCodeId());

        // Verify
        verify(qrCodeRepository).findByQrId(qrId);
        verify(walletService).getWalletByNumber(otherWallet.getWalletNumber());
        verify(transactionService).processQRCodeTransaction(any(Transaction.class));
    }

    @Test
    public void testProcessQRCodePayment_QRCodeInactive() {
        // Set QR code as inactive
        testQRCode.setActive(false);

        when(qrCodeRepository.findByQrId(anyString())).thenReturn(Optional.of(testQRCode));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> qrCodeService.processQRCodePayment(
                otherUser, qrId, BigDecimal.valueOf(50), otherWallet.getWalletNumber()));

        // Verify
        verify(qrCodeRepository).findByQrId(qrId);
        verify(transactionService, never()).processQRCodeTransaction(any(Transaction.class));
    }

    @Test
    public void testProcessQRCodePayment_QRCodeExpired() {
        // Set QR code as expired
        testQRCode.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(qrCodeRepository.findByQrId(anyString())).thenReturn(Optional.of(testQRCode));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> qrCodeService.processQRCodePayment(
                otherUser, qrId, BigDecimal.valueOf(50), otherWallet.getWalletNumber()));

        // Verify
        verify(qrCodeRepository).findByQrId(qrId);
        verify(transactionService, never()).processQRCodeTransaction(any(Transaction.class));
    }

    @Test
    public void testDeactivateQRCode_Success() {
        // Arrange
        when(qrCodeRepository.findByQrId(anyString())).thenReturn(Optional.of(testQRCode));
        when(qrCodeRepository.save(any(QRCode.class))).thenReturn(testQRCode);

        // Act
        qrCodeService.deactivateQRCode(testUser, qrId);

        // Assert
        assertFalse(testQRCode.isActive());

        // Verify
        verify(qrCodeRepository).findByQrId(qrId);
        verify(qrCodeRepository).save(testQRCode);
    }

    @Test
    public void testDeactivateQRCode_NotOwner() {
        // Arrange
        when(qrCodeRepository.findByQrId(anyString())).thenReturn(Optional.of(testQRCode));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> qrCodeService.deactivateQRCode(otherUser, qrId));

        // Verify
        verify(qrCodeRepository).findByQrId(qrId);
        verify(qrCodeRepository, never()).save(any(QRCode.class));
    }

    @Test
    public void testGenerateQRCodeImage() {
        // This test is minimal since it involves external libraries
        when(qrCodeRepository.findByQrId(anyString())).thenReturn(Optional.of(testQRCode));

        // Act
        String base64Image = qrCodeService.generateQRCodeImage(qrId);

        // Assert
        assertNotNull(base64Image);
        assertTrue(base64Image.length() > 0);

        // Verify
        verify(qrCodeRepository).findByQrId(qrId);
    }
}
