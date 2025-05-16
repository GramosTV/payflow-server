package com.payflow.api.model.dto.response;

import com.payflow.api.model.entity.Wallet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private Long id;
    private String walletNumber;
    private String currency;
    private BigDecimal balance;
    private LocalDateTime createdAt;

    public static WalletResponse fromEntity(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getWalletNumber(),
                wallet.getCurrency().name(),
                wallet.getBalance(),
                wallet.getCreatedAt());
    }
}
