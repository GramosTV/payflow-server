package com.payflow.api.security;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Authentication entry point for JWT-based authentication. Handles unauthorized access attempts.
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  /** Private constructor to prevent direct instantiation. */
  private JwtAuthenticationEntryPoint() {
    // Spring will create instances
  }

  @Override
  public void commence(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final AuthenticationException authException)
      throws IOException {
    if (log.isErrorEnabled()) {
      log.error("Unauthorized error: {}", authException.getMessage());
    }
    response.sendError(
        HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: " + authException.getMessage());
  }
}
