package com.payflow.api.model.dto.request;

import com.payflow.api.model.entity.PaymentMethod;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PaymentMethodRequest {

    @NotNull(message = "Payment method type is required")
    private PaymentMethod.PaymentMethodType type;

    @NotBlank(message = "Payment method name is required")
    private String name;

    // Card-specific fields
    private String cardNumber;
    private String expiryDate;
    private String cvv;

    // Bank account-specific fields
    private String accountNumber;
    private String routingNumber;
}
