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
@Table(name = "qr_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QRCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String qrId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private boolean isAmountFixed;

    @Column(nullable = false)
    private boolean isOneTime;

    private String description;
    @Column(nullable = false)
    private boolean isActive = true;

    // Removing incorrect OneToOne relationship with Transaction
    // QR code ID is stored in Transaction entity as a string field, not as a direct
    // relationship

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime expiresAt;

    public QRCode(Wallet wallet, BigDecimal amount, boolean isAmountFixed, boolean isOneTime, String description,
            LocalDateTime expiresAt) {
        this.wallet = wallet;
        this.amount = amount;
        this.isAmountFixed = isAmountFixed;
        this.isOneTime = isOneTime;
        this.description = description;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    public void generateQrId() {
        if (qrId == null || qrId.isEmpty()) {
            this.qrId = "QR-" + UUID.randomUUID().toString().substring(0, 12);
        }
    }
}
