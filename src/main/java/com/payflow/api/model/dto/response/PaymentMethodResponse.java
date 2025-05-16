package com.payflow.api.model.dto.response;

import com.payflow.api.model.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {

    private Long id;
    private String type;
    private String name;
    private String lastFourDigits;
    private LocalDateTime createdAt;

    public static PaymentMethodResponse fromEntity(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(
                paymentMethod.getId(),
                paymentMethod.getType().name(),
                paymentMethod.getName(),
                paymentMethod.getLastFourDigits(),
                paymentMethod.getCreatedAt());
    }
}
