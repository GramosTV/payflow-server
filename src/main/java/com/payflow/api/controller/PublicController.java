package com.payflow.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for public endpoints that don't require authentication. */
@RestController
@RequestMapping("public")
@Tag(
    name = "Public Endpoints",
    description = "Public API endpoints that don't require authentication")
public class PublicController {

  /**
   * Health check endpoint.
   *
   * @return health status information
   */
  @GetMapping("/health")
  @Operation(summary = "Health check endpoint")
  public ResponseEntity<Map<String, Object>> healthCheck() {
    final Map<String, Object> response = new HashMap<>();
    response.put("status", "UP");
    response.put("timestamp", LocalDateTime.now());
    response.put("service", "PayFlow Lite API");
    return ResponseEntity.ok(response);
  }

  /**
   * Get available currencies.
   *
   * @return list of available currencies
   */
  @GetMapping("/currencies")
  @Operation(summary = "Get available currencies")
  public ResponseEntity<Map<String, Object>> getAvailableCurrencies() {
    final Map<String, Object> response = new HashMap<>();
    response.put(
        "currencies",
        java.util.Arrays.asList(com.payflow.api.model.entity.Wallet.Currency.values()));

    return ResponseEntity.ok(response);
  }
}
