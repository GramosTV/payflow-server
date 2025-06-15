package com.payflow.api.model.dto.request;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class SignUpRequest {

  @NotBlank(message = "Full name is required")
  @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
  private String fullName;

  @NotBlank(message = "Email is required")
  @Email(message = "Email should be valid")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 6, message = "Password must be at least 6 characters")
  private String password;

  private String phoneNumber;
}
