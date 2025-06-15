package com.payflow.api.exception;

import java.math.BigDecimal;

/** Exception thrown when a transaction cannot be completed due to insufficient funds */
public class InsufficientFundsException extends BadRequestException {

  private static final long serialVersionUID = 1L;

  private final BigDecimal availableBalance;
  private final BigDecimal requestedAmount;

  public InsufficientFundsException(BigDecimal availableBalance, BigDecimal requestedAmount) {
    super(
        "Insufficient funds: available balance "
            + availableBalance
            + " is less than requested amount "
            + requestedAmount);
    this.availableBalance = availableBalance;
    this.requestedAmount = requestedAmount;
  }

  public BigDecimal getAvailableBalance() {
    return availableBalance;
  }

  public BigDecimal getRequestedAmount() {
    return requestedAmount;
  }
}
