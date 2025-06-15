package com.payflow.api.model.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QRCodeRequest {
  private String walletNumber;
  private BigDecimal amount;
  private boolean isAmountFixed;
  private boolean isOneTime;
  private String description;
  private LocalDateTime expiresAt;
}
