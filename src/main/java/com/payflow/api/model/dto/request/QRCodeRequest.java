package com.payflow.api.model.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QRCodeRequest {
    private String walletNumber;
    private BigDecimal amount;
    private boolean isAmountFixed;
    private boolean isOneTime;
    private String description;
    private LocalDateTime expiresAt;
}
