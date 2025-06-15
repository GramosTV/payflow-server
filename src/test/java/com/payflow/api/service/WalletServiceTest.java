package com.payflow.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.payflow.api.exception.BadRequestException;
import com.payflow.api.exception.ResourceNotFoundException;
import com.payflow.api.model.dto.request.TopUpRequest;
import com.payflow.api.model.dto.request.WalletRequest;
import com.payflow.api.model.entity.Transaction;
import com.payflow.api.model.entity.User;
import com.payflow.api.model.entity.Wallet;
import com.payflow.api.repository.WalletRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class WalletServiceTest {

  @Mock private WalletRepository walletRepository;

  @Mock private TransactionService transactionService;

  @InjectMocks private WalletService walletService;

  private User testUser;
  private Wallet testWallet;
  private Transaction testTransaction;
  private WalletRequest walletRequest;
  private TopUpRequest topUpRequest;

  @BeforeEach
  public void setup() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setEmail("test@example.com");
    testUser.setFullName("Test User");

    testWallet = new Wallet();
    testWallet.setId(1L);
    testWallet.setUser(testUser);
    testWallet.setCurrency(Wallet.Currency.USD);
    testWallet.setBalance(BigDecimal.valueOf(1000));
    testWallet.setWalletNumber("WALLET123456");
    testWallet.setCreatedAt(LocalDateTime.now()); // Changed from Instant.now()

    testTransaction = new Transaction();
    testTransaction.setId(1L);
    testTransaction.setTransactionNumber("TXN123456");
    testTransaction.setAmount(BigDecimal.valueOf(500));
    testTransaction.setType(Transaction.TransactionType.DEPOSIT);

    walletRequest = new WalletRequest();
    walletRequest.setCurrency(Wallet.Currency.EUR);
    walletRequest.setInitialDeposit(BigDecimal.valueOf(200));

    topUpRequest = new TopUpRequest();
    topUpRequest.setWalletNumber("WALLET123456");
    topUpRequest.setAmount(BigDecimal.valueOf(500));
  }

  @Test
  public void testCreateDefaultWallet() {
    // Arrange
    when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

    // Act
    Wallet result = walletService.createDefaultWallet(testUser);

    // Assert
    assertNotNull(result);
    assertEquals(testUser, result.getUser());
    assertEquals(Wallet.Currency.USD, result.getCurrency());

    // Verify
    verify(walletRepository).save(any(Wallet.class));
    verify(transactionService, never()).createDepositTransaction(any(), any());
  }

  @Test
  public void testCreateWallet_Success() {
    // Arrange
    when(walletRepository.findByUserAndCurrency(any(User.class), any(Wallet.Currency.class)))
        .thenReturn(Optional.empty());
    when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
    when(transactionService.createDepositTransaction(any(Wallet.class), any(BigDecimal.class)))
        .thenReturn(testTransaction);

    // Act
    Wallet result = walletService.createWallet(testUser, walletRequest);

    // Assert
    assertNotNull(result);
    assertEquals(testUser, result.getUser());

    // Verify
    verify(walletRepository).findByUserAndCurrency(testUser, walletRequest.getCurrency());
    verify(walletRepository).save(any(Wallet.class));
    verify(transactionService)
        .createDepositTransaction(any(Wallet.class), eq(walletRequest.getInitialDeposit()));
  }

  @Test
  public void testCreateWallet_AlreadyExists() {
    // Arrange
    when(walletRepository.findByUserAndCurrency(any(User.class), any(Wallet.Currency.class)))
        .thenReturn(Optional.of(testWallet));

    // Act & Assert
    assertThrows(
        BadRequestException.class, () -> walletService.createWallet(testUser, walletRequest));

    // Verify
    verify(walletRepository).findByUserAndCurrency(testUser, walletRequest.getCurrency());
    verify(walletRepository, never()).save(any(Wallet.class));
    verify(transactionService, never()).createDepositTransaction(any(), any());
  }

  @Test
  public void testGetUserWallets() {
    // Arrange
    List<Wallet> wallets = Arrays.asList(testWallet);
    when(walletRepository.findByUserOrderByCreatedAtDesc(any(User.class))).thenReturn(wallets);

    // Act
    List<Wallet> result = walletService.getUserWallets(testUser);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(testWallet.getId(), result.get(0).getId());

    // Verify
    verify(walletRepository).findByUserOrderByCreatedAtDesc(testUser);
  }

  @Test
  public void testGetWalletById_Success() {
    // Arrange
    when(walletRepository.findById(anyLong())).thenReturn(Optional.of(testWallet));

    // Act
    Wallet result = walletService.getWalletById(1L);

    // Assert
    assertNotNull(result);
    assertEquals(testWallet.getId(), result.getId());

    // Verify
    verify(walletRepository).findById(1L);
  }

  @Test
  public void testGetWalletById_NotFound() {
    // Arrange
    when(walletRepository.findById(anyLong())).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class, () -> walletService.getWalletById(999L));

    // Verify
    verify(walletRepository).findById(999L);
  }

  @Test
  public void testGetWalletByNumber_Success() {
    // Arrange
    when(walletRepository.findByWalletNumber(anyString())).thenReturn(Optional.of(testWallet));

    // Act
    Wallet result = walletService.getWalletByNumber("WALLET123456");

    // Assert
    assertNotNull(result);
    assertEquals(testWallet.getWalletNumber(), result.getWalletNumber());

    // Verify
    verify(walletRepository).findByWalletNumber("WALLET123456");
  }

  @Test
  public void testGetWalletByNumber_NotFound() {
    // Arrange
    when(walletRepository.findByWalletNumber(anyString())).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class, () -> walletService.getWalletByNumber("INVALID"));

    // Verify
    verify(walletRepository).findByWalletNumber("INVALID");
  }

  @Test
  public void testTopUpWallet_Success() {
    // Arrange
    when(walletRepository.findByWalletNumber(anyString())).thenReturn(Optional.of(testWallet));
    when(transactionService.createDepositTransaction(any(Wallet.class), any(BigDecimal.class)))
        .thenReturn(testTransaction);
    when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

    // Act
    Transaction result = walletService.topUpWallet(testUser, topUpRequest);

    // Assert
    assertNotNull(result);
    assertEquals(testTransaction.getId(), result.getId());

    // Verify
    verify(walletRepository).findByWalletNumber(topUpRequest.getWalletNumber());
    verify(transactionService).createDepositTransaction(testWallet, topUpRequest.getAmount());
    verify(walletRepository).save(testWallet);
  }

  @Test
  public void testTopUpWallet_NotOwnWallet() {
    // Create a different user
    User differentUser = new User();
    differentUser.setId(2L);

    // Set up the wallet to belong to the test user (id=1)
    when(walletRepository.findByWalletNumber(anyString())).thenReturn(Optional.of(testWallet));

    // Act & Assert - should throw exception when different user (id=2) tries to top
    // up
    assertThrows(
        BadRequestException.class, () -> walletService.topUpWallet(differentUser, topUpRequest));

    // Verify
    verify(walletRepository).findByWalletNumber(topUpRequest.getWalletNumber());
    verify(transactionService, never()).createDepositTransaction(any(), any());
    verify(walletRepository, never()).save(any(Wallet.class));
  }

  @Test
  public void testUpdateWalletBalance() {
    // Arrange
    when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
    BigDecimal initialBalance = testWallet.getBalance();
    BigDecimal amountToAdd = BigDecimal.valueOf(250);

    // Act
    walletService.updateWalletBalance(testWallet, amountToAdd);

    // Assert
    assertEquals(initialBalance.add(amountToAdd), testWallet.getBalance());

    // Verify
    verify(walletRepository).save(testWallet);
  }
}
