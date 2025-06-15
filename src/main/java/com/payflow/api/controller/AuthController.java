package com.payflow.api.controller;

import com.payflow.api.model.dto.request.LoginRequest;
import com.payflow.api.model.dto.request.SignUpRequest;
import com.payflow.api.model.dto.response.JwtAuthResponse;
import com.payflow.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for authentication operations. Handles user registration and login endpoints. */
@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/signup")
  @Operation(summary = "Register a new user")
  public ResponseEntity<JwtAuthResponse> registerUser(
      @Valid @RequestBody final SignUpRequest signUpRequest) {
    return new ResponseEntity<>(authService.register(signUpRequest), HttpStatus.CREATED);
  }

  @PostMapping("/login")
  @Operation(summary = "Login a user")
  public ResponseEntity<JwtAuthResponse> loginUser(
      @Valid @RequestBody final LoginRequest loginRequest) {
    return ResponseEntity.ok(authService.login(loginRequest));
  }
}
