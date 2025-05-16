package com.payflow.api.service;

import com.payflow.api.exception.BadRequestException;
import com.payflow.api.exception.ResourceNotFoundException;
import com.payflow.api.model.dto.request.TransactionRequest;
import com.payflow.api.model.entity.MoneyRequest;
import com.payflow.api.model.entity.Transaction;
import com.payflow.api.model.entity.User;
import com.payflow.api.model.entity.Wallet;
import com.payflow.api.repository.TransactionRepository;
import com.payflow.api.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ExchangeRateService exchangeRateService;
    private final WalletRepository walletRepository;

    /**
     * Generic method to create a transaction. This can be used by other services
     * or specific transaction creation methods within this service.
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
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED); // Assuming direct completion for now
        transaction.setDescription(description);

        // Handle exchange rate if currencies are different and both wallets are present
        if (sourceWallet != null && destinationWallet != null && !sourceCurrency.equals(destinationCurrency)) {
            BigDecimal exchangeRate = exchangeRateService.getExchangeRate(sourceCurrency, destinationCurrency);
            transaction.setExchangeRate(exchangeRate);
        }

        return transactionRepository.save(transaction);
    }

    /**
     * Create a deposit transaction (add funds to wallet)
     */
    @Transactional
    public Transaction createDepositTransaction(Wallet wallet, BigDecimal amount) {
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

        return transactionRepository.save(transaction);
    }

    /**
     * Transfer money between wallets
     */
    @Transactional
    public Transaction createTransferTransaction(User sender, TransactionRequest request) {
        Wallet sourceWallet = walletRepository.findByWalletNumber(request.getSourceWalletNumber())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Wallet", "walletNumber", request.getSourceWalletNumber()));

        Wallet destinationWallet = walletRepository.findByWalletNumber(request.getDestinationWalletNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "walletNumber",
                        request.getDestinationWalletNumber()));

        // Check if source wallet belongs to sender
        if (!sourceWallet.getUser().getId().equals(sender.getId())) {
            throw new BadRequestException("You can only send money from your own wallet");
        }

        // Check if source wallet has enough balance
        if (sourceWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BadRequestException("Insufficient balance in source wallet");
        }

        // Calculate exchange rate if currencies are different
        BigDecimal exchangeRate = BigDecimal.ONE;
        BigDecimal convertedAmount = request.getAmount();

        if (!sourceWallet.getCurrency().equals(destinationWallet.getCurrency())) {
            exchangeRate = exchangeRateService.getExchangeRate(
                    sourceWallet.getCurrency(),
                    destinationWallet.getCurrency());
            convertedAmount = exchangeRateService.convertCurrency(
                    request.getAmount(),
                    sourceWallet.getCurrency(),
                    destinationWallet.getCurrency());
        }

        // Create transaction
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

        transaction = transactionRepository.save(transaction);

        // Update wallet balances
        sourceWallet.setBalance(sourceWallet.getBalance().subtract(request.getAmount()));
        destinationWallet.setBalance(destinationWallet.getBalance().add(convertedAmount));

        walletRepository.save(sourceWallet);
        walletRepository.save(destinationWallet);

        return transaction;
    }

    /**
     * Create a transaction for money request payment
     */
    @Transactional
    public Transaction createMoneyRequestTransaction(MoneyRequest moneyRequest, Wallet sourceWallet) {
        User sender = sourceWallet.getUser();
        User receiver = moneyRequest.getRequester();
        Wallet destinationWallet = moneyRequest.getRequestWallet();
        BigDecimal amount = moneyRequest.getAmount();

        // Check if source wallet has enough balance
        if (sourceWallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient balance in source wallet");
        }

        // Calculate exchange rate if currencies are different
        BigDecimal exchangeRate = BigDecimal.ONE;
        BigDecimal convertedAmount = amount;

        if (!sourceWallet.getCurrency().equals(destinationWallet.getCurrency())) {
            exchangeRate = exchangeRateService.getExchangeRate(
                    sourceWallet.getCurrency(),
                    destinationWallet.getCurrency());
            convertedAmount = exchangeRateService.convertCurrency(
                    amount,
                    sourceWallet.getCurrency(),
                    destinationWallet.getCurrency());
        }

        // Create transaction
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

        transaction = transactionRepository.save(transaction);

        // Update wallet balances
        sourceWallet.setBalance(sourceWallet.getBalance().subtract(amount));
        destinationWallet.setBalance(destinationWallet.getBalance().add(convertedAmount));

        walletRepository.save(sourceWallet);
        walletRepository.save(destinationWallet);

        return transaction;
    }

    /**
     * Process a QR code transaction
     */
    @Transactional
    public Transaction processQRCodeTransaction(Transaction transaction) {
        Wallet sourceWallet = transaction.getSourceWallet();
        Wallet destinationWallet = transaction.getDestinationWallet();
        BigDecimal amount = transaction.getAmount();

        // Calculate exchange rate if currencies are different
        BigDecimal exchangeRate = BigDecimal.ONE;
        BigDecimal convertedAmount = amount;

        if (!sourceWallet.getCurrency().equals(destinationWallet.getCurrency())) {
            exchangeRate = exchangeRateService.getExchangeRate(
                    sourceWallet.getCurrency(),
                    destinationWallet.getCurrency());
            convertedAmount = exchangeRateService.convertCurrency(
                    amount,
                    sourceWallet.getCurrency(),
                    destinationWallet.getCurrency());
            transaction.setExchangeRate(exchangeRate);
        }

        // Save transaction
        transaction = transactionRepository.save(transaction);

        // Update wallet balances
        sourceWallet.setBalance(sourceWallet.getBalance().subtract(amount));
        destinationWallet.setBalance(destinationWallet.getBalance().add(convertedAmount));

        walletRepository.save(sourceWallet);
        walletRepository.save(destinationWallet);

        return transaction;
    }

    /**
     * Get transaction by ID
     */
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
    }

    /**
     * Get transaction by transaction number
     */
    public Transaction getTransactionByNumber(String transactionNumber) {
        return transactionRepository.findByTransactionNumber(transactionNumber)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Transaction", "transactionNumber", transactionNumber));
    }

    /**
     * Get transactions for a user
     */
    public Page<Transaction> getUserTransactions(User user, Pageable pageable) {
        return transactionRepository.findBySenderOrReceiverOrderByCreatedAtDesc(user, user, pageable);
    }

    /**
     * Get transactions for a wallet
     */
    public Page<Transaction> getWalletTransactions(Wallet wallet, Pageable pageable) {
        return transactionRepository.findByWalletOrderByCreatedAtDesc(wallet, pageable);
    }

    /**
     * Get transactions for a user within a date range
     */
    public List<Transaction> getUserTransactionsInDateRange(User user, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByUserAndDateRange(user, startDate, endDate);
    }
}
