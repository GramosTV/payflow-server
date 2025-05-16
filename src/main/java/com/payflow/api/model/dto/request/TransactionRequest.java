package com.payflow.api.model.dto.request;

import com.payflow.api.model.entity.Wallet;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class TransactionRequest {

    @NotBlank(message = "Source wallet number is required")
    private String sourceWalletNumber;

    @NotBlank(message = "Destination wallet number is required")
    private String destinationWalletNumber;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String description;
}
