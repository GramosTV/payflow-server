package com.payflow.api.controller;

import com.payflow.api.exception.PayflowApiException;
import com.payflow.api.exception.ResourceNotFoundException;
import com.payflow.api.model.dto.request.QRCodeRequest;
import com.payflow.api.model.entity.QRCode;
import com.payflow.api.model.entity.Transaction;
import com.payflow.api.model.entity.User;
import com.payflow.api.security.UserPrincipal;
import com.payflow.api.service.QRCodeService;
import com.payflow.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Controller for QR code operations. */
@RestController
@RequestMapping("qr-codes")
@Tag(name = "QR Codes", description = "QR Code management API")
@RequiredArgsConstructor
@Slf4j
public class QRCodeController {

  private final QRCodeService qrCodeService;
  private final UserService userService;

  /**
   * Creates a new QR code for a wallet.
   *
   * @param currentUser the authenticated user
   * @param qrCodeRequest the QR code request details
   * @return the created QR code
   */
  @PostMapping
  @Operation(summary = "Create a new QR code for a wallet")
  public ResponseEntity<?> createQrCode(
      @AuthenticationPrincipal final UserPrincipal currentUser,
      @RequestBody final QRCodeRequest qrCodeRequest) {

    final User user = userService.getUserById(currentUser.getId());
    final QRCode qrCode =
        qrCodeService.createWalletQRCode(
            user,
            qrCodeRequest.getWalletNumber(),
            qrCodeRequest.getAmount(),
            qrCodeRequest.isAmountFixed(),
            qrCodeRequest.isOneTime(),
            qrCodeRequest.getDescription(),
            qrCodeRequest.getExpiresAt());

    final Map<String, Object> response = new HashMap<>();
    response.put("id", qrCode.getId());
    response.put("qrId", qrCode.getQrId());
    response.put("walletNumber", qrCode.getWallet().getWalletNumber());
    response.put("currency", qrCode.getWallet().getCurrency());
    response.put("amount", qrCode.getAmount());
    response.put("isAmountFixed", qrCode.isAmountFixed());
    response.put("isOneTime", qrCode.isOneTime());
    response.put("description", qrCode.getDescription());
    response.put("isActive", qrCode.isActive());
    response.put("expiresAt", qrCode.getExpiresAt());
    response.put("createdAt", qrCode.getCreatedAt());

    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  /**
   * Retrieves all QR codes for the current user.
   *
   * @param currentUser the authenticated user
   * @return list of QR codes
   */
  @GetMapping
  @Operation(summary = "Get all QR codes for the current user")
  public ResponseEntity<?> getMyQrCodes(@AuthenticationPrincipal final UserPrincipal currentUser) {
    final User user = userService.getUserById(currentUser.getId());
    final List<QRCode> qrCodes = qrCodeService.getUserQRCodes(user);

    final List<Map<String, Object>> response =
        qrCodes.stream()
            .map(
                qrCode -> {
                  final Map<String, Object> map = new HashMap<>();
                  map.put("id", qrCode.getId());
                  map.put("qrId", qrCode.getQrId());
                  map.put("walletNumber", qrCode.getWallet().getWalletNumber());
                  map.put("currency", qrCode.getWallet().getCurrency());
                  map.put("amount", qrCode.getAmount());
                  map.put("isAmountFixed", qrCode.isAmountFixed());
                  map.put("isOneTime", qrCode.isOneTime());
                  map.put("description", qrCode.getDescription());
                  map.put("isActive", qrCode.isActive());
                  map.put("expiresAt", qrCode.getExpiresAt());
                  map.put("createdAt", qrCode.getCreatedAt());
                  return map;
                })
            .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}/image")
  @Operation(
      summary = "Get QR code image by ID as base64 string",
      description = "Returns a base64 encoded PNG image of the QR code for the given ID")
  @io.swagger.v3.oas.annotations.responses.ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "QR code image generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "QR code not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Error generating QR code image")
      })
  public ResponseEntity<Map<String, String>> getQRCodeImageById(@PathVariable Long id) {
    try {
      // First retrieve the QR code object with wallet eagerly loaded
      QRCode qrCode = qrCodeService.getQRCodeByIdWithWallet(id);
      if (qrCode == null) {
        throw new ResourceNotFoundException("QRCode", "id", id);
      }

      // Generate the image using the QR code's unique identifier
      String base64Image = qrCodeService.generateQRCodeImage(qrCode.getQrId());

      Map<String, String> response = new HashMap<>();
      response.put("id", id.toString());
      response.put("qrId", qrCode.getQrId());
      response.put("imageData", "data:image/png;base64," + base64Image);

      return ResponseEntity.ok(response);
    } catch (ResourceNotFoundException e) {
      // Log the not found error
      log.warn("QR code not found: {}", e.getMessage());

      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("error", "QR code not found: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    } catch (Exception e) {
      // Log the error
      System.err.println("Error generating QR code image: " + e.getMessage());
      e.printStackTrace();

      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("error", "Failed to generate QR code image: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get QR code by ID",
      description = "Retrieves a QR code by its ID with detailed information")
  @io.swagger.v3.oas.annotations.responses.ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "QR code found and returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Current user does not own the QR code"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "QR code not found")
      })
  public ResponseEntity<?> getQRCodeById(
      @AuthenticationPrincipal UserPrincipal currentUser, @PathVariable Long id) {
    User user = userService.getUserById(currentUser.getId());

    // Use getQRCodeByIdWithWallet to prevent LazyInitializationException
    QRCode qrCode = qrCodeService.getQRCodeByIdWithWallet(id);

    // Ensure the QR code belongs to the current user
    if (!qrCode.getWallet().getUser().getId().equals(user.getId())) {
      throw new PayflowApiException("You don't have access to this QR code", HttpStatus.FORBIDDEN);
    }

    Map<String, Object> response = new HashMap<>();
    response.put("id", qrCode.getId());
    response.put("qrId", qrCode.getQrId());
    response.put("walletNumber", qrCode.getWallet().getWalletNumber());
    response.put("currency", qrCode.getWallet().getCurrency());
    response.put("amount", qrCode.getAmount());
    response.put("isAmountFixed", qrCode.isAmountFixed());
    response.put("isOneTime", qrCode.isOneTime());
    response.put("description", qrCode.getDescription());
    response.put("isActive", qrCode.isActive());
    response.put("expiresAt", qrCode.getExpiresAt());
    response.put("createdAt", qrCode.getCreatedAt());

    // Generate and add the QR code image
    String base64Image = qrCodeService.generateQRCodeImage(qrCode.getQrId());
    response.put("qrCodeImage", "data:image/png;base64," + base64Image);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/{qrId}/pay")
  @Operation(summary = "Pay using a QR code")
  public ResponseEntity<?> payWithQRCode(
      @AuthenticationPrincipal UserPrincipal currentUser,
      @PathVariable String qrId,
      @RequestBody Map<String, Object> paymentData) {

    User user = userService.getUserById(currentUser.getId());

    BigDecimal amount =
        paymentData.get("amount") != null
            ? new BigDecimal(paymentData.get("amount").toString())
            : null;

    String sourceWalletNumber = (String) paymentData.get("sourceWalletNumber");
    // String paymentMethodId = (String) paymentData.get("paymentMethodId");
    // TODO: Add payment method processing in future implementation

    Transaction transaction =
        qrCodeService.processQRCodePayment(user, qrId, amount, sourceWalletNumber);

    Map<String, Object> response = new HashMap<>();
    response.put("transactionId", transaction.getId());
    response.put("transactionNumber", transaction.getTransactionNumber());
    response.put("status", transaction.getStatus());
    response.put("amount", transaction.getAmount());
    response.put("createdAt", transaction.getCreatedAt());

    return ResponseEntity.ok(response);
  }

  @PostMapping("/{id}/deactivate")
  @Operation(summary = "Deactivate a QR code")
  public ResponseEntity<?> deactivateQRCode(
      @AuthenticationPrincipal UserPrincipal currentUser, @PathVariable Long id) {

    User user = userService.getUserById(currentUser.getId());
    // Use getQRCodeByIdWithWallet to prevent LazyInitializationException
    QRCode qrCode = qrCodeService.getQRCodeByIdWithWallet(id);
    qrCodeService.deactivateQRCode(user, qrCode.getQrId());

    // Return the updated QR code to match client expectations
    Map<String, Object> response = new HashMap<>();
    response.put("id", qrCode.getId());
    response.put("qrId", qrCode.getQrId());
    response.put("walletNumber", qrCode.getWallet().getWalletNumber());
    response.put("currency", qrCode.getWallet().getCurrency());
    response.put("amount", qrCode.getAmount());
    response.put("isAmountFixed", qrCode.isAmountFixed());
    response.put("isOneTime", qrCode.isOneTime());
    response.put("description", qrCode.getDescription());
    response.put("isActive", false);
    response.put("expiresAt", qrCode.getExpiresAt());
    response.put("createdAt", qrCode.getCreatedAt());

    return ResponseEntity.ok(response);
  }
}
