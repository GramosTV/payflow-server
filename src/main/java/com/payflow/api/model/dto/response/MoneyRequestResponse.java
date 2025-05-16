package com.payflow.api.model.dto.response;

import com.payflow.api.model.entity.MoneyRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoneyRequestResponse {
    private Long id;
    private String requestNumber;
    private String requesterName;
    private String requesterEmail;
    private String requesteeName;
    private String requesteeEmail;
    private String walletNumber;
    private String currency;
    private BigDecimal amount;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public static MoneyRequestResponse fromEntity(MoneyRequest moneyRequest) {
        return new MoneyRequestResponse(
                moneyRequest.getId(),
                moneyRequest.getRequestNumber(),
                moneyRequest.getRequester().getFullName(),
                moneyRequest.getRequester().getEmail(),
                moneyRequest.getRequestee().getFullName(),
                moneyRequest.getRequestee().getEmail(),
                moneyRequest.getRequestWallet().getWalletNumber(),
                moneyRequest.getRequestWallet().getCurrency().name(),
                moneyRequest.getAmount(),
                moneyRequest.getStatus().name(),
                moneyRequest.getDescription(),
                moneyRequest.getCreatedAt(),
                moneyRequest.getExpiresAt());
    }
}
