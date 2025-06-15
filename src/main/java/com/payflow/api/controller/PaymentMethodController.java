package com.payflow.api.controller;

import com.payflow.api.model.dto.request.PaymentMethodRequest;
import com.payflow.api.model.dto.response.PaymentMethodResponse;
import com.payflow.api.model.entity.PaymentMethod;
import com.payflow.api.model.entity.User;
import com.payflow.api.security.UserPrincipal;
import com.payflow.api.service.PaymentMethodService;
import com.payflow.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("payment-methods")
@RequiredArgsConstructor
@Tag(name = "Payment Methods", description = "Payment Method management API")
public class PaymentMethodController {

  private final PaymentMethodService paymentMethodService;
  private final UserService userService;

  @GetMapping
  @Operation(summary = "Get all payment methods for the current user")
  public ResponseEntity<List<PaymentMethodResponse>> getMyPaymentMethods(
      @AuthenticationPrincipal UserPrincipal currentUser) {
    User user = userService.getUserById(currentUser.getId());
    List<PaymentMethod> paymentMethods = paymentMethodService.getUserPaymentMethods(user);

    List<PaymentMethodResponse> responses =
        paymentMethods.stream().map(PaymentMethodResponse::fromEntity).collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  @PostMapping
  @Operation(summary = "Create a new payment method")
  public ResponseEntity<PaymentMethodResponse> createPaymentMethod(
      @AuthenticationPrincipal UserPrincipal currentUser,
      @Valid @RequestBody PaymentMethodRequest request) {

    User user = userService.getUserById(currentUser.getId());

    // Convert DTO to entity
    PaymentMethod paymentMethod = new PaymentMethod();
    paymentMethod.setType(request.getType());
    paymentMethod.setName(request.getName());
    paymentMethod.setCardNumber(request.getCardNumber());
    paymentMethod.setExpiryDate(request.getExpiryDate());
    paymentMethod.setCvv(request.getCvv());
    paymentMethod.setAccountNumber(request.getAccountNumber());
    paymentMethod.setRoutingNumber(request.getRoutingNumber());

    paymentMethod = paymentMethodService.createPaymentMethod(user, paymentMethod);

    return new ResponseEntity<>(
        PaymentMethodResponse.fromEntity(paymentMethod), HttpStatus.CREATED);
  }

  @DeleteMapping("/{paymentMethodId}")
  @Operation(summary = "Delete a payment method")
  public ResponseEntity<Void> deletePaymentMethod(
      @AuthenticationPrincipal UserPrincipal currentUser, @PathVariable Long paymentMethodId) {

    User user = userService.getUserById(currentUser.getId());
    paymentMethodService.deletePaymentMethod(user, paymentMethodId);

    return ResponseEntity.noContent().build();
  }
}
