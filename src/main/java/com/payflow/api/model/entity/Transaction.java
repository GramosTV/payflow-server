package com.payflow.api.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String transactionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = true)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_wallet_id", nullable = false)
    private Wallet sourceWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_wallet_id", nullable = true)
    private Wallet destinationWallet;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private String description;

    @Column(precision = 19, scale = 4)
    private BigDecimal exchangeRate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Wallet.Currency sourceCurrency;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Wallet.Currency destinationCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "money_request_id")
    private MoneyRequest moneyRequest;

    @Column(name = "qr_code_id")
    private String qrCodeId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum TransactionType {
        DEPOSIT, TRANSFER, WITHDRAWAL, CONVERSION, REQUEST_PAYMENT
    }

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, CANCELLED
    }

    @PrePersist
    public void generateTransactionNumber() {
        if (transactionNumber == null || transactionNumber.isEmpty()) {
            this.transactionNumber = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }
}
