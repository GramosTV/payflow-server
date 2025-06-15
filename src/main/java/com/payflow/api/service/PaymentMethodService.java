package com.payflow.api.service;

import com.payflow.api.exception.BadRequestException;
import com.payflow.api.exception.ResourceNotFoundException;
import com.payflow.api.model.entity.PaymentMethod;
import com.payflow.api.model.entity.User;
import com.payflow.api.repository.PaymentMethodRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

  private final PaymentMethodRepository paymentMethodRepository;

  public List<PaymentMethod> getUserPaymentMethods(User user) {
    return paymentMethodRepository.findByUserOrderByCreatedAtDesc(user);
  }

  public PaymentMethod getPaymentMethodById(Long id) {
    return paymentMethodRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", id));
  }

  @Transactional
  public PaymentMethod createPaymentMethod(User user, PaymentMethod paymentMethod) {
    if (paymentMethod.getType() == PaymentMethod.PaymentMethodType.CARD) {
      if (paymentMethod.getCardNumber() == null
          || paymentMethod.getExpiryDate() == null
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
