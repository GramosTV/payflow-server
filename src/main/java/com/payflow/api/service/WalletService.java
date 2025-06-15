package com.payflow.api.service;

import com.payflow.api.exception.BadRequestException;
import com.payflow.api.exception.ResourceNotFoundException;
import com.payflow.api.model.dto.request.TopUpRequest;
import com.payflow.api.model.dto.request.WalletRequest;
import com.payflow.api.model.entity.Transaction;
import com.payflow.api.model.entity.User;
import com.payflow.api.model.entity.Wallet;
import com.payflow.api.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service class for managing wallets. */
@Service
@RequiredArgsConstructor
public class WalletService {

  private final WalletRepository walletRepository;
  private final TransactionService transactionService;

  /**
   * Creates a default wallet for a user with USD currency.
   *
   * @param user the user to create wallet for
   * @return the created wallet
   */
  @Transactional
  public Wallet createDefaultWallet(final User user) {
    return createWallet(user, Wallet.Currency.USD, BigDecimal.ZERO);
  }

  /**
   * Creates a new wallet for a user based on request.
   *
   * @param user the user to create wallet for
   * @param walletRequest the wallet creation request
   * @return the created wallet
   * @throws BadRequestException if user already has wallet in that currency
   */
  @Transactional
  public Wallet createWallet(final User user, final WalletRequest walletRequest) {
    if (walletRepository.findByUserAndCurrency(user, walletRequest.getCurrency()).isPresent()) {
      throw new BadRequestException(
          "You already have a wallet in " + walletRequest.getCurrency() + " currency");
    }

    return createWallet(user, walletRequest.getCurrency(), walletRequest.getInitialDeposit());
  }

  /**
   * Creates a wallet with specified currency and initial balance.
   *
   * @param user the user to create wallet for
   * @param currency the wallet currency
   * @param initialBalance the initial balance * @return the created wallet
   */
  @Transactional
  public Wallet createWallet(
      final User user, final Wallet.Currency currency, final BigDecimal initialBalance) {
    final Wallet wallet = new Wallet();
    wallet.setUser(user);
    wallet.setCurrency(currency);
    wallet.setBalance(initialBalance);

    final Wallet savedWallet = walletRepository.save(wallet);
    if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
      transactionService.createDepositTransaction(savedWallet, initialBalance);
    }
    return savedWallet;
  }

  /**
   * Retrieves all wallets for a user.
   *
   * @param user the user
   * @return list of user's wallets
   */
  public List<Wallet> getUserWallets(final User user) {
    return walletRepository.findByUserOrderByCreatedAtDesc(user);
  }

  /**
   * Retrieves a wallet by its ID.
   *
   * @param walletId the wallet ID
   * @return the wallet
   * @throws ResourceNotFoundException if wallet not found
   */
  public Wallet getWalletById(final Long walletId) {
    return walletRepository
        .findById(walletId)
        .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", walletId));
  }

  /**
   * Retrieves a wallet by its wallet number.
   *
   * @param walletNumber the wallet number
   * @return the wallet
   * @throws ResourceNotFoundException if wallet not found
   */
  public Wallet getWalletByNumber(final String walletNumber) {
    return walletRepository
        .findByWalletNumber(walletNumber)
        .orElseThrow(() -> new ResourceNotFoundException("Wallet", "walletNumber", walletNumber));
  }

  /**
   * Tops up a wallet with specified amount.
   *
   * @param user the user performing the top-up
   * @param topUpRequest the top-up request details
   * @return the transaction record
   * @throws BadRequestException if user doesn't own the wallet
   */
  @Transactional
  public Transaction topUpWallet(final User user, final TopUpRequest topUpRequest) {
    final Wallet wallet = getWalletByNumber(topUpRequest.getWalletNumber());
    if (!wallet.getUser().getId().equals(user.getId())) {
      throw new BadRequestException("You can only top up your own wallet");
    }
    final Transaction transaction =
        transactionService.createDepositTransaction(wallet, topUpRequest.getAmount());
    wallet.setBalance(wallet.getBalance().add(topUpRequest.getAmount()));
    walletRepository.save(wallet);

    return transaction;
  }

  @Transactional
  public Wallet depositFunds(User user, BigDecimal amount, Long paymentMethodId) {
    // For simplicity, assume deposit to the primary USD wallet.
    // You might need more sophisticated logic if users can choose which wallet to
    // deposit into.
    Wallet primaryWallet =
        getUserWallets(user).stream()
            .filter(w -> w.getCurrency() == Wallet.Currency.USD)
            .findFirst()
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Wallet", "type", "Primary USD")); // Here, you would
    // typically
    // interact with a
    // payment gateway
    // using the
    // paymentMethodId
    // to charge the user. Since that's out of scope for this example, we'll
    // simulate a successful payment.

    transactionService.createDepositTransaction(primaryWallet, amount);

    primaryWallet.setBalance(primaryWallet.getBalance().add(amount));
    walletRepository.save(primaryWallet);

    // The transaction object itself might be more appropriate to return,
    // or a dedicated response DTO. Returning Wallet for now as per controller.
    return primaryWallet;
  }

  @Transactional
  public Wallet withdrawFunds(User user, BigDecimal amount, Long paymentMethodId) {
    // Again, assuming withdrawal from the primary USD wallet for simplicity.
    Wallet primaryWallet =
        getUserWallets(user).stream()
            .filter(w -> w.getCurrency() == Wallet.Currency.USD)
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Wallet", "type", "Primary USD"));

    if (primaryWallet.getBalance().compareTo(amount) < 0) {
      throw new BadRequestException("Insufficient funds for withdrawal.");
    } // Here, you would typically interact with a payment gateway using the
    // paymentMethodId
    // to process the withdrawal. This is a simplified simulation.

    // Create withdrawal transaction (you might need a new transaction type or
    // adjust existing)
    // For now, using a generic description. Consider creating a specific withdrawal
    // transaction method in TransactionService.
    transactionService.createTransaction(
        primaryWallet, // sourceWallet
        null, // destinationWallet (null for external withdrawal)
        amount,
        primaryWallet.getCurrency(), // sourceCurrency
        primaryWallet.getCurrency(), // destinationCurrency (same for withdrawal)
        Transaction.TransactionType.WITHDRAWAL, // type
        "Withdrawal to payment method " + paymentMethodId // description
        );

    // Update wallet balance
    primaryWallet.setBalance(primaryWallet.getBalance().subtract(amount));
    walletRepository.save(primaryWallet);

    return primaryWallet;
  }

  @Transactional
  public void updateWalletBalance(Wallet wallet, BigDecimal amount) {
    wallet.setBalance(wallet.getBalance().add(amount));
    walletRepository.save(wallet);
  }
}
