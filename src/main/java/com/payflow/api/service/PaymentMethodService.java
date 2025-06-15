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

/** Service class for managing payment methods. */
@Service
@RequiredArgsConstructor
public class PaymentMethodService {

  private final PaymentMethodRepository paymentMethodRepository;

  /**
   * Retrieves all payment methods for a user.
   *
   * @param user the user whose payment methods to retrieve
   * @return list of payment methods ordered by creation date
   */
  public List<PaymentMethod> getUserPaymentMethods(final User user) {
    return paymentMethodRepository.findByUserOrderByCreatedAtDesc(user);
  }

  /**
   * Retrieves a payment method by its ID.
   *
   * @param id the payment method ID
   * @return the payment method
   * @throws ResourceNotFoundException if payment method not found
   */
  public PaymentMethod getPaymentMethodById(final Long id) {
    return paymentMethodRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", id));
  }

  /**
   * Creates a new payment method for a user.
   *
   * @param user the user who owns the payment method
   * @param paymentMethod the payment method to create
   * @return the created payment method
   * @throws BadRequestException if required fields are missing
   */
  @Transactional
  public PaymentMethod createPaymentMethod(final User user, final PaymentMethod paymentMethod) {
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

  /**
   * Deletes a payment method for a user.
   *
   * @param user the user who owns the payment method
   * @param paymentMethodId the ID of the payment method to delete
   * @throws BadRequestException if user doesn't own the payment method
   * @throws ResourceNotFoundException if payment method not found
   */
  @Transactional
  public void deletePaymentMethod(final User user, final Long paymentMethodId) {
    final PaymentMethod paymentMethod = getPaymentMethodById(paymentMethodId);

    // Ensure the payment method belongs to the user
    if (!paymentMethod.getUser().getId().equals(user.getId())) {
      throw new BadRequestException("You can only delete your own payment methods");
    }

    paymentMethodRepository.delete(paymentMethod);
  }
}
