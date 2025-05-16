package com.payflow.api.model.dto.request;

import com.payflow.api.model.entity.Wallet;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class WalletRequest {

    @NotNull(message = "Currency is required")
    private Wallet.Currency currency;

    @NotNull(message = "Initial deposit amount is required")
    @Positive(message = "Initial deposit must be positive")
    private BigDecimal initialDeposit;
}
