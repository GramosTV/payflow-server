package com.payflow.api.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "money_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoneyRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String requestNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requester_id", nullable = false)
  private User requester;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requestee_id", nullable = false)
  private User requestee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "wallet_id", nullable = false)
  private Wallet requestWallet;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal amount;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private RequestStatus status = RequestStatus.PENDING;

  private String description;

  @OneToOne(mappedBy = "moneyRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Transaction transaction;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Column private LocalDateTime expiresAt;

  public enum RequestStatus {
    PENDING,
    APPROVED,
    DECLINED,
    EXPIRED,
    CANCELLED
  }

  @PrePersist
  public void generateRequestNumber() {
    if (requestNumber == null || requestNumber.isEmpty()) {
      this.requestNumber =
          "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
    }

    // Set expiration date 7 days from creation
    if (expiresAt == null) {
      this.expiresAt = LocalDateTime.now().plusDays(7);
    }
  }
}
