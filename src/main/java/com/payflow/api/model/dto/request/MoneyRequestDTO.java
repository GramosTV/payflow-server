package com.payflow.api.model.dto.request;

import java.math.BigDecimal;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Data;

@Data
public class MoneyRequestDTO {

  @NotBlank(message = "Requestee email is required")
  @Email(message = "Invalid email format")
  private String requesteeEmail;

  @NotBlank(message = "Wallet number is required")
  private String walletNumber;

  @NotNull(message = "Amount is required")
  @Positive(message = "Amount must be positive")
  private BigDecimal amount;

  private String description;
}
