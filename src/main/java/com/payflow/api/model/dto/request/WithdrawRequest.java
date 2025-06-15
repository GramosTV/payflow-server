package com.payflow.api.model.dto.request;

import java.math.BigDecimal;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Data;

@Data
public class WithdrawRequest {

  @NotNull(message = "Amount is required")
  @Positive(message = "Amount must be positive")
  private BigDecimal amount;

  @NotNull(message = "Payment method ID is required for withdrawal")
  private Long paymentMethodId;
}
