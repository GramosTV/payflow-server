package com.payflow.api.service;

import com.payflow.api.exception.BadRequestException;
import com.payflow.api.exception.InsufficientFundsException;
import com.payflow.api.exception.PayflowApiException;
import com.payflow.api.exception.ResourceNotFoundException;
import com.payflow.api.model.dto.request.TransactionRequest;
import com.payflow.api.model.entity.MoneyRequest;
import com.payflow.api.model.entity.Transaction;
import com.payflow.api.model.entity.User;
import com.payflow.api.model.entity.Wallet;
import com.payflow.api.repository.TransactionRepository;
import com.payflow.api.repository.WalletRepository;
import com.payflow.api.repository.projection.TransactionSummary;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Service for handling all transaction-related operations */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final ExchangeRateService exchangeRateService;
  private final WalletRepository walletRepository;

  /**
   * Generic method to create a transaction. This can be used by other services or specific
   * transaction creation methods within this service.
   *
   * @param sourceWallet Source wallet (can be null for external deposits)
   * @param destinationWallet Destination wallet (can be null for external withdrawals)
   * @param amount Transaction amount
   * @param sourceCurrency Source currency
   * @param destinationCurrency Destination currency
   * @param type Transaction type
   * @param description Description of the transaction
   * @return Created transaction entity
   */
  @Transactional
  public Transaction createTransaction(
      Wallet sourceWallet,
      Wallet destinationWallet,
      BigDecimal amount,
      Wallet.Currency sourceCurrency,
      Wallet.Currency destinationCurrency,
      Transaction.TransactionType type,
      String description) {

    log.info("Creating transaction of type {} for amount {}", type, amount);

    try {
      User sender = (sourceWallet != null) ? sourceWallet.getUser() : null;
      User receiver = (destinationWallet != null) ? destinationWallet.getUser() : null;

      Transaction transaction = new Transaction();
      transaction.setSender(sender);
      transaction.setReceiver(receiver);
      transaction.setSourceWallet(sourceWallet);
      transaction.setDestinationWallet(destinationWallet);
      transaction.setAmount(amount);
      transaction.setSourceCurrency(sourceCurrency);
      transaction.setDestinationCurrency(destinationCurrency);
      transaction.setType(type);
      transaction.setStatus(
          Transaction.TransactionStatus.COMPLETED); // Assuming direct completion for now
      transaction.setDescription(description);

      // Handle exchange rate if currencies are different and both wallets are present
      if (sourceWallet != null
          && destinationWallet != null
          && !sourceCurrency.equals(destinationCurrency)) {
        BigDecimal exchangeRate =
            exchangeRateService.getExchangeRate(sourceCurrency, destinationCurrency);
        transaction.setExchangeRate(exchangeRate);
        log.debug(
            "Applied exchange rate of {} from {} to {}",
            exchangeRate,
            sourceCurrency,
            destinationCurrency);
      }

      Transaction savedTransaction = transactionRepository.save(transaction);
      log.info("Successfully created transaction with ID: {}", savedTransaction.getId());
      return savedTransaction;
    } catch (Exception e) {
      log.error("Error creating transaction: {}", e.getMessage(), e);
      throw new PayflowApiException(
          "Failed to create transaction", e, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Create a deposit transaction (add funds to wallet)
   *
   * @param wallet Wallet to deposit funds to
   * @param amount Amount to deposit
   * @return Created transaction entity
   */
  @Transactional
  public Transaction createDepositTransaction(Wallet wallet, BigDecimal amount) {
    log.info(
        "Creating deposit transaction for wallet {} with amount {}",
        wallet.getWalletNumber(),
        amount);

    try {
      User user = wallet.getUser();

      Transaction transaction = new Transaction();
      transaction.setSender(user);
      transaction.setReceiver(user);
      transaction.setSourceWallet(wallet);
      transaction.setDestinationWallet(wallet);
      transaction.setAmount(amount);
      transaction.setSourceCurrency(wallet.getCurrency());
      transaction.setDestinationCurrency(wallet.getCurrency());
      transaction.setType(Transaction.TransactionType.DEPOSIT);
      transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
      transaction.setDescription("Deposit to wallet " + wallet.getWalletNumber());
      wallet.setBalance(wallet.getBalance().add(amount));
      walletRepository.save(wallet);

      Transaction savedTransaction = transactionRepository.save(transaction);
      log.info("Successfully created deposit transaction with ID: {}", savedTransaction.getId());
      return savedTransaction;

    } catch (Exception e) {
      log.error("Error creating deposit transaction: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to create deposit transaction", e);
    }
  }

  /**
   * Transfer money between wallets
   *
   * @param sender User making the transfer
   * @param request Transfer request details
   * @return Created transaction entity
   * @throws ResourceNotFoundException If source or destination wallet not found
   * @throws BadRequestException If sender doesn't own source wallet
   * @throws InsufficientFundsException If source wallet has insufficient balance
   */
  @Transactional(isolation = Isolation.REPEATABLE_READ)
  public Transaction createTransferTransaction(User sender, TransactionRequest request) {
    log.info(
        "Creating transfer transaction from {} to {} with amount {}",
        request.getSourceWalletNumber(),
        request.getDestinationWalletNumber(),
        request.getAmount());
    Wallet sourceWallet =
        walletRepository
            .findByWalletNumberWithLock(request.getSourceWalletNumber())
            .orElseThrow(
                () -> {
                  log.error("Source wallet not found: {}", request.getSourceWalletNumber());
                  return new ResourceNotFoundException(
                      "Wallet", "walletNumber", request.getSourceWalletNumber());
                });

    Wallet destinationWallet =
        walletRepository
            .findByWalletNumber(request.getDestinationWalletNumber())
            .orElseThrow(
                () -> {
                  log.error(
                      "Destination wallet not found: {}", request.getDestinationWalletNumber());
                  return new ResourceNotFoundException(
                      "Wallet", "walletNumber", request.getDestinationWalletNumber());
                });

    if (!sourceWallet.getUser().getId().equals(sender.getId())) {
      log.error(
          "User {} attempted to send money from wallet {} that they don't own",
          sender.getId(),
          sourceWallet.getId());
      throw new BadRequestException("You can only send money from your own wallet");
    }

    if (sourceWallet.getBalance().compareTo(request.getAmount()) < 0) {
      log.error(
          "Insufficient balance in wallet {}: requested {} but available {}",
          sourceWallet.getId(),
          request.getAmount(),
          sourceWallet.getBalance());
      throw new InsufficientFundsException(sourceWallet.getBalance(), request.getAmount());
    }
    try {
      BigDecimal exchangeRate = BigDecimal.ONE;
      BigDecimal convertedAmount = request.getAmount();

      if (!sourceWallet.getCurrency().equals(destinationWallet.getCurrency())) {
        exchangeRate =
            exchangeRateService.getExchangeRate(
                sourceWallet.getCurrency(), destinationWallet.getCurrency());
        convertedAmount =
            exchangeRateService.convertCurrency(
                request.getAmount(), sourceWallet.getCurrency(), destinationWallet.getCurrency());
        log.debug(
            "Applied exchange rate of {} from {} to {}, converted amount: {}",
            exchangeRate,
            sourceWallet.getCurrency(),
            destinationWallet.getCurrency(),
            convertedAmount);
      }

      Transaction transaction = new Transaction();
      transaction.setSender(sender);
      transaction.setReceiver(destinationWallet.getUser());
      transaction.setSourceWallet(sourceWallet);
      transaction.setDestinationWallet(destinationWallet);
      transaction.setAmount(request.getAmount());
      transaction.setSourceCurrency(sourceWallet.getCurrency());
      transaction.setDestinationCurrency(destinationWallet.getCurrency());
      transaction.setType(Transaction.TransactionType.TRANSFER);
      transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
      transaction.setDescription(request.getDescription());
      transaction.setExchangeRate(exchangeRate);

      Transaction savedTransaction = transactionRepository.save(transaction);

      sourceWallet.setBalance(sourceWallet.getBalance().subtract(request.getAmount()));
      destinationWallet.setBalance(destinationWallet.getBalance().add(convertedAmount));

      walletRepository.save(sourceWallet);
      walletRepository.save(destinationWallet);

      log.info("Successfully created transfer transaction with ID: {}", savedTransaction.getId());
      return savedTransaction;

    } catch (Exception e) {
      log.error("Error creating transfer transaction: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to create transfer transaction", e);
    }
  }

  /**
   * Create a transaction for money request payment
   *
   * @param moneyRequest Money request to pay
   * @param sourceWallet Wallet to pay from
   * @return Created transaction entity
   * @throws InsufficientFundsException If source wallet has insufficient balance
   */
  @Transactional(isolation = Isolation.REPEATABLE_READ)
  public Transaction createMoneyRequestTransaction(MoneyRequest moneyRequest, Wallet sourceWallet) {
    log.info(
        "Processing money request payment from wallet {} for request ID {}",
        sourceWallet.getWalletNumber(),
        moneyRequest.getId());

    User sender = sourceWallet.getUser();
    User receiver = moneyRequest.getRequester();
    Wallet destinationWallet = moneyRequest.getRequestWallet();
    BigDecimal amount = moneyRequest.getAmount();

    // Check if source wallet has enough balance
    if (sourceWallet.getBalance().compareTo(amount) < 0) {
      log.error(
          "Insufficient balance in wallet {}: requested {} but available {}",
          sourceWallet.getId(),
          amount,
          sourceWallet.getBalance());
      throw new InsufficientFundsException(sourceWallet.getBalance(), amount);
    }
    try {
      BigDecimal exchangeRate = BigDecimal.ONE;
      BigDecimal convertedAmount = amount;

      if (!sourceWallet.getCurrency().equals(destinationWallet.getCurrency())) {
        exchangeRate =
            exchangeRateService.getExchangeRate(
                sourceWallet.getCurrency(), destinationWallet.getCurrency());
        convertedAmount =
            exchangeRateService.convertCurrency(
                amount, sourceWallet.getCurrency(), destinationWallet.getCurrency());
        log.debug(
            "Applied exchange rate of {} from {} to {}, converted amount: {}",
            exchangeRate,
            sourceWallet.getCurrency(),
            destinationWallet.getCurrency(),
            convertedAmount);
      }

      Transaction transaction = new Transaction();
      transaction.setSender(sender);
      transaction.setReceiver(receiver);
      transaction.setSourceWallet(sourceWallet);
      transaction.setDestinationWallet(destinationWallet);
      transaction.setAmount(amount);
      transaction.setSourceCurrency(sourceWallet.getCurrency());
      transaction.setDestinationCurrency(destinationWallet.getCurrency());
      transaction.setType(Transaction.TransactionType.REQUEST_PAYMENT);
      transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
      transaction.setDescription("Payment for request: " + moneyRequest.getDescription());
      transaction.setExchangeRate(exchangeRate);
      transaction.setMoneyRequest(moneyRequest);

      Transaction savedTransaction = transactionRepository.save(transaction);

      sourceWallet.setBalance(sourceWallet.getBalance().subtract(amount));
      destinationWallet.setBalance(destinationWallet.getBalance().add(convertedAmount));

      walletRepository.save(sourceWallet);
      walletRepository.save(destinationWallet);

      log.info(
          "Successfully created money request transaction with ID: {}", savedTransaction.getId());
      return savedTransaction;

    } catch (Exception e) {
      log.error("Error creating money request transaction: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to create money request transaction", e);
    }
  }

  /**
   * Process a QR code transaction
   *
   * @param transaction Prepared transaction entity
   * @return Processed transaction entity
   */
  @Transactional(isolation = Isolation.REPEATABLE_READ)
  public Transaction processQRCodeTransaction(Transaction transaction) {
    log.info(
        "Processing QR code transaction between wallets {} and {}",
        transaction.getSourceWallet().getWalletNumber(),
        transaction.getDestinationWallet().getWalletNumber());

    Wallet sourceWallet = transaction.getSourceWallet();
    Wallet destinationWallet = transaction.getDestinationWallet();
    BigDecimal amount = transaction.getAmount();

    // Check if source wallet has enough balance
    if (sourceWallet.getBalance().compareTo(amount) < 0) {
      log.error(
          "Insufficient balance in wallet {}: requested {} but available {}",
          sourceWallet.getId(),
          amount,
          sourceWallet.getBalance());
      throw new InsufficientFundsException(sourceWallet.getBalance(), amount);
    }
    try {
      BigDecimal convertedAmount = amount;

      if (!sourceWallet.getCurrency().equals(destinationWallet.getCurrency())) {
        BigDecimal exchangeRate =
            exchangeRateService.getExchangeRate(
                sourceWallet.getCurrency(), destinationWallet.getCurrency());
        convertedAmount =
            exchangeRateService.convertCurrency(
                amount, sourceWallet.getCurrency(), destinationWallet.getCurrency());
        transaction.setExchangeRate(exchangeRate);
        log.debug(
            "Applied exchange rate of {} from {} to {}, converted amount: {}",
            exchangeRate,
            sourceWallet.getCurrency(),
            destinationWallet.getCurrency(),
            convertedAmount);
      }

      Transaction savedTransaction = transactionRepository.save(transaction);

      sourceWallet.setBalance(sourceWallet.getBalance().subtract(amount));
      destinationWallet.setBalance(destinationWallet.getBalance().add(convertedAmount));

      walletRepository.save(sourceWallet);
      walletRepository.save(destinationWallet);

      log.info("Successfully processed QR code transaction with ID: {}", savedTransaction.getId());
      return savedTransaction;

    } catch (Exception e) {
      log.error("Error processing QR code transaction: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to process QR code transaction", e);
    }
  }

  @Transactional(readOnly = true)
  public Transaction getTransactionById(Long id) {
    log.debug("Retrieving transaction with ID: {}", id);
    return transactionRepository
        .findById(id)
        .orElseThrow(
            () -> {
              log.error("Transaction not found with ID: {}", id);
              return new ResourceNotFoundException("Transaction", "id", id);
            });
  }

  @Transactional(readOnly = true)
  public Transaction getTransactionByNumber(String transactionNumber) {
    log.debug("Retrieving transaction with number: {}", transactionNumber);
    return transactionRepository
        .findByTransactionNumber(transactionNumber)
        .orElseThrow(
            () -> {
              log.error("Transaction not found with number: {}", transactionNumber);
              return new ResourceNotFoundException(
                  "Transaction", "transactionNumber", transactionNumber);
            });
  }

  @Transactional(readOnly = true)
  public Page<Transaction> getUserTransactions(User user, Pageable pageable) {
    log.debug("Retrieving transactions for user ID: {}", user.getId());
    return transactionRepository.findBySenderOrReceiverOrderByCreatedAtDesc(user, pageable);
  }

  @Transactional(readOnly = true)
  public Page<TransactionSummary> getUserTransactionSummaries(User user, Pageable pageable) {
    log.debug("Retrieving transaction summaries for user ID: {}", user.getId());
    return transactionRepository.findSummariesByUserId(user.getId(), pageable);
  }

  @Transactional(readOnly = true)
  public Page<Transaction> getWalletTransactions(Wallet wallet, Pageable pageable) {
    log.debug("Retrieving transactions for wallet ID: {}", wallet.getId());
    return transactionRepository.findByWalletOrderByCreatedAtDesc(wallet, pageable);
  }

  @Transactional(readOnly = true)
  public List<Transaction> getUserTransactionsInDateRange(
      User user, LocalDateTime startDate, LocalDateTime endDate) {
    log.debug(
        "Retrieving transactions for user ID: {} between {} and {}",
        user.getId(),
        startDate,
        endDate);
    return transactionRepository.findByUserAndDateRange(user, startDate, endDate);
  }
}
