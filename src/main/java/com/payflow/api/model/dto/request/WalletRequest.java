package com.payflow.api.model.dto.request;

import com.payflow.api.model.entity.Wallet;
import java.math.BigDecimal;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Data;

@Data
public class WalletRequest {

  @NotNull(message = "Currency is required")
  private Wallet.Currency currency;

  @NotNull(message = "Initial deposit amount is required")
  @Positive(message = "Initial deposit must be positive")
  private BigDecimal initialDeposit;
}
