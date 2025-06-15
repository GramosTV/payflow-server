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

@RestController
@RequestMapping("public")
@Tag(
    name = "Public Endpoints",
    description = "Public API endpoints that don't require authentication")
public class PublicController {

  @GetMapping("/health")
  @Operation(summary = "Health check endpoint")
  public ResponseEntity<Map<String, Object>> healthCheck() {
    Map<String, Object> response = new HashMap<>();
    response.put("status", "UP");
    response.put("timestamp", LocalDateTime.now());
    response.put("service", "PayFlow Lite API");

    return ResponseEntity.ok(response);
  }

  @GetMapping("/currencies")
  @Operation(summary = "Get available currencies")
  public ResponseEntity<Map<String, Object>> getAvailableCurrencies() {
    Map<String, Object> response = new HashMap<>();
    response.put(
        "currencies",
        java.util.Arrays.asList(com.payflow.api.model.entity.Wallet.Currency.values()));

    return ResponseEntity.ok(response);
  }
}
