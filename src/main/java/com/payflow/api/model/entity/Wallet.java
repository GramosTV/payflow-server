package com.payflow.api.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Currency currency;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal balance = BigDecimal.ZERO;

  @Column(nullable = false, unique = true)
  private String walletNumber;

  @OneToMany(mappedBy = "sourceWallet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Transaction> outgoingTransactions = new ArrayList<>();

  @OneToMany(mappedBy = "destinationWallet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Transaction> incomingTransactions = new ArrayList<>();

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  public enum Currency {
    USD,
    EUR,
    GBP,
    PLN,
    JPY,
    CAD,
    AUD,
    CHF,
    CNY,
    INR
  }

  private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

  @PrePersist
  public void generateWalletNumber() {
    if (walletNumber == null || walletNumber.isEmpty()) {
      // Format: PF-{Currency}-{RandomDigits}
      String randomDigits = String.format("%010d", SECURE_RANDOM.nextInt(1000000000));
      this.walletNumber = "PF-" + currency + "-" + randomDigits;
    }
  }
}
