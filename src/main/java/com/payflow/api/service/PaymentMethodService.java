package com.payflow.api.service;

import com.payflow.api.exception.BadRequestException;
import com.payflow.api.exception.ResourceNotFoundException;
import com.payflow.api.model.entity.PaymentMethod;
import com.payflow.api.model.entity.User;
import com.payflow.api.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    /**
     * Get all payment methods for a user
     */
    public List<PaymentMethod> getUserPaymentMethods(User user) {
        return paymentMethodRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Get payment method by ID
     */
    public PaymentMethod getPaymentMethodById(Long id) {
        return paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", id));
    }

    /**
     * Create a new payment method
     */
    @Transactional
    public PaymentMethod createPaymentMethod(User user, PaymentMethod paymentMethod) {
        // Validate based on type
        if (paymentMethod.getType() == PaymentMethod.PaymentMethodType.CARD) {
            if (paymentMethod.getCardNumber() == null || paymentMethod.getExpiryDate() == null
                    || paymentMethod.getCvv() == null) {
                throw new BadRequestException(
                        "Card number, expiry date, and CVV are required for card payment methods");
            }
        } else if (paymentMethod.getType() == PaymentMethod.PaymentMethodType.BANK_ACCOUNT) {
            if (paymentMethod.getAccountNumber() == null || paymentMethod.getRoutingNumber() == null) {
                throw new BadRequestException(
                        "Account number and routing number are required for bank account payment methods");
            }
        }

        paymentMethod.setUser(user);
        return paymentMethodRepository.save(paymentMethod);
    }

    /**
     * Delete a payment method
     */
    @Transactional
    public void deletePaymentMethod(User user, Long paymentMethodId) {
        PaymentMethod paymentMethod = getPaymentMethodById(paymentMethodId);

        // Ensure the payment method belongs to the user
        if (!paymentMethod.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only delete your own payment methods");
        }

        paymentMethodRepository.delete(paymentMethod);
    }
}
