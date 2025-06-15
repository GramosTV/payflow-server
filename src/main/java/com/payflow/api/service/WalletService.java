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

@Service
@RequiredArgsConstructor
public class WalletService {

  private final WalletRepository walletRepository;
  private final TransactionService transactionService;

  @Transactional
  public Wallet createDefaultWallet(User user) {
    return createWallet(user, Wallet.Currency.USD, BigDecimal.ZERO);
  }

  @Transactional
  public Wallet createWallet(User user, WalletRequest walletRequest) {
    if (walletRepository.findByUserAndCurrency(user, walletRequest.getCurrency()).isPresent()) {
      throw new BadRequestException(
          "You already have a wallet in " + walletRequest.getCurrency() + " currency");
    }

    return createWallet(user, walletRequest.getCurrency(), walletRequest.getInitialDeposit());
  }

  @Transactional
  public Wallet createWallet(User user, Wallet.Currency currency, BigDecimal initialBalance) {
    Wallet wallet = new Wallet();
    wallet.setUser(user);
    wallet.setCurrency(currency);
    wallet.setBalance(initialBalance);

    wallet = walletRepository.save(wallet);
    if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
      transactionService.createDepositTransaction(wallet, initialBalance);
    }

    return wallet;
  }

  public List<Wallet> getUserWallets(User user) {
    return walletRepository.findByUserOrderByCreatedAtDesc(user);
  }

  public Wallet getWalletById(Long walletId) {
    return walletRepository
        .findById(walletId)
        .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", walletId));
  }

  public Wallet getWalletByNumber(String walletNumber) {
    return walletRepository
        .findByWalletNumber(walletNumber)
        .orElseThrow(() -> new ResourceNotFoundException("Wallet", "walletNumber", walletNumber));
  }

  @Transactional
  public Transaction topUpWallet(User user, TopUpRequest topUpRequest) {
    Wallet wallet = getWalletByNumber(topUpRequest.getWalletNumber());
    if (!wallet.getUser().getId().equals(user.getId())) {
      throw new BadRequestException("You can only top up your own wallet");
    }
    Transaction transaction =
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
