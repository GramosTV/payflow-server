package com.payflow.api.model.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class MoneyRequestActionDTO {

    @NotBlank(message = "Request number is required")
    private String requestNumber;

    @NotBlank(message = "Action is required")
    private String action; // APPROVE, DECLINE

    @NotBlank(message = "Wallet number to make payment from is required")
    private String paymentWalletNumber;
}
