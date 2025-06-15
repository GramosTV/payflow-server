package com.payflow.api.model.entity;

import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "payment_methods")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private PaymentMethodType type;

  @Column(nullable = false)
  private String name;

  @Column(name = "card_number")
  private String cardNumber;

  @Column(name = "expiry_date")
  private String expiryDate;

  private String cvv;

  @Column(name = "account_number")
  private String accountNumber;

  @Column(name = "routing_number")
  private String routingNumber;

  @Column(name = "last_four_digits")
  private String lastFourDigits;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  public enum PaymentMethodType {
    CARD,
    BANK_ACCOUNT
  }

  @PrePersist
  public void setLastFourDigits() {
    if (this.type == PaymentMethodType.CARD
        && this.cardNumber != null
        && this.cardNumber.length() >= 4) {
      this.lastFourDigits = this.cardNumber.substring(this.cardNumber.length() - 4);
    } else if (this.type == PaymentMethodType.BANK_ACCOUNT
        && this.accountNumber != null
        && this.accountNumber.length() >= 4) {
      this.lastFourDigits = this.accountNumber.substring(this.accountNumber.length() - 4);
    }
  }
}
