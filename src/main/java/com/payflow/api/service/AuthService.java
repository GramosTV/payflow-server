package com.payflow.api.service;

import com.payflow.api.exception.BadRequestException;
import com.payflow.api.model.dto.request.LoginRequest;
import com.payflow.api.model.dto.request.SignUpRequest;
import com.payflow.api.model.dto.response.JwtAuthResponse;
import com.payflow.api.model.entity.User;
import com.payflow.api.security.JwtTokenProvider;
import com.payflow.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service class for authentication operations. */
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserService userService;
  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider tokenProvider;
  private final WalletService walletService;

  /**
   * Registers a new user and creates a default wallet.
   *
   * @param signUpRequest the registration request
   * @return JWT authentication response
   */
  @Transactional
  public JwtAuthResponse register(final SignUpRequest signUpRequest) {
    final User user = userService.createUser(signUpRequest);
    walletService.createDefaultWallet(user);

    return authenticateUser(signUpRequest.getEmail(), signUpRequest.getPassword());
  }

  /**
   * Authenticates a user login.
   *
   * @param loginRequest the login request
   * @return JWT authentication response
   */
  public JwtAuthResponse login(final LoginRequest loginRequest) {
    return authenticateUser(loginRequest.getEmail(), loginRequest.getPassword());
  }

  private JwtAuthResponse authenticateUser(final String email, final String password) {
    try {
      final Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(email, password));

      SecurityContextHolder.getContext().setAuthentication(authentication);

      final String jwt = tokenProvider.generateToken(authentication);
      final UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

      return new JwtAuthResponse(
          jwt,
          "Bearer", // Added tokenType parameter
          userPrincipal.getId(),
          userPrincipal.getEmail(),
          userPrincipal.getFullName(),
          userPrincipal.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));
    } catch (Exception e) {
      throw new BadRequestException("Invalid email or password");
    }
  }
}
