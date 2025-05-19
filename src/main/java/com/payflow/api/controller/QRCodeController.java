package com.payflow.api.controller;

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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("qr-codes")
@RequiredArgsConstructor
@Tag(name = "QR Codes", description = "QR Code management API")
public class QRCodeController {

    private final QRCodeService qrCodeService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a new QR code for a wallet")
    public ResponseEntity<?> createQRCode(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody QRCodeRequest qrCodeRequest) {

        User user = userService.getUserById(currentUser.getId());
        QRCode qrCode = qrCodeService.createWalletQRCode(
                user,
                qrCodeRequest.getWalletNumber(),
                qrCodeRequest.getAmount(),
                qrCodeRequest.isAmountFixed(),
                qrCodeRequest.isOneTime(),
                qrCodeRequest.getDescription(),
                qrCodeRequest.getExpiresAt());

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

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all QR codes for the current user")
    public ResponseEntity<?> getMyQRCodes(@AuthenticationPrincipal UserPrincipal currentUser) {
        User user = userService.getUserById(currentUser.getId());
        List<QRCode> qrCodes = qrCodeService.getUserQRCodes(user);

        List<Map<String, Object>> response = qrCodes.stream().map(qrCode -> {
            Map<String, Object> map = new HashMap<>();
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
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/image")
    @Operation(summary = "Get QR code image by ID as base64 string")
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
            System.err.println("QR code not found: " + e.getMessage());

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
    @Operation(summary = "Get QR code by ID")
    public ResponseEntity<?> getQRCodeById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {

        User user = userService.getUserById(currentUser.getId());
        // Use getQRCodeByIdWithWallet to prevent LazyInitializationException
        QRCode qrCode = qrCodeService.getQRCodeByIdWithWallet(id);

        // Ensure the QR code belongs to the current user
        if (!qrCode.getWallet().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have access to this QR code");
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

        BigDecimal amount = paymentData.get("amount") != null ? new BigDecimal(paymentData.get("amount").toString())
                : null;

        String sourceWalletNumber = (String) paymentData.get("sourceWalletNumber");
        // String paymentMethodId = (String) paymentData.get("paymentMethodId");
        // TODO: Add payment method processing in future implementation

        Transaction transaction = qrCodeService.processQRCodePayment(user, qrId, amount, sourceWalletNumber);

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
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {

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
