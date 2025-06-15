package com.payflow.api.model.dto.response;

import com.payflow.api.model.entity.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
  private Long id;
  private String transactionNumber;
  private String senderName;
  private String receiverName;
  private String sourceWalletNumber;
  private String destinationWalletNumber;
  private BigDecimal amount;
  private BigDecimal exchangeRate;
  private String sourceCurrency;
  private String destinationCurrency;
  private String type;
  private String status;
  private String description;
  private LocalDateTime createdAt;

  public static TransactionResponse fromEntity(Transaction transaction) {
    return new TransactionResponse(
        transaction.getId(),
        transaction.getTransactionNumber(),
        transaction.getSender().getFullName(),
        transaction.getReceiver().getFullName(),
        transaction.getSourceWallet().getWalletNumber(),
        transaction.getDestinationWallet().getWalletNumber(),
        transaction.getAmount(),
        transaction.getExchangeRate(),
        transaction.getSourceCurrency().name(),
        transaction.getDestinationCurrency().name(),
        transaction.getType().name(),
        transaction.getStatus().name(),
        transaction.getDescription(),
        transaction.getCreatedAt());
  }
}
