package com.payflow.api.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Projection interface for optimized Transaction queries This reduces the amount of data fetched
 * when full Transaction entities are not needed
 */
public interface TransactionSummary {
  Long getId();

  String getTransactionNumber();

  BigDecimal getAmount();

  String getType();

  String getStatus();

  LocalDateTime getCreatedAt();

  String getSenderName();

  String getReceiverName();
}
