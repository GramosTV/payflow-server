package com.payflow.api.security;

import io.jsonwebtoken.*;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** JWT token provider for authentication and authorization. */
@Component
@Slf4j
public class JwtTokenProvider {

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${jwt.expiration}")
  private int jwtExpirationInMs;

  /**
   * Generates a JWT token for authenticated user.
   *
   * @param authentication the authentication object
   * @return JWT token string
   */
  public String generateToken(final Authentication authentication) {
    final UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

    final Date now = new Date();
    final Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

    return Jwts.builder()
        .setSubject(Long.toString(userPrincipal.getId()))
        .setIssuedAt(new Date())
        .setExpiration(expiryDate)
        .claim("email", userPrincipal.getUsername())
        .claim("role", userPrincipal.getAuthorities().iterator().next().getAuthority())
        .signWith(SignatureAlgorithm.HS512, jwtSecret)
        .compact();
  }

  /**
   * Extracts user ID from JWT token.
   *
   * @param token the JWT token
   * @return user ID
   */
  public Long getUserIdFromJwt(final String token) {
    final Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();

    return Long.parseLong(claims.getSubject());
  }

  /**
   * Validates a JWT token.
   *
   * @param authToken the token to validate
   * @return true if valid, false otherwise
   */
  public boolean validateToken(final String authToken) {
    try {
      Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
      return true;
    } catch (SignatureException ex) {
      log.error("Invalid JWT signature");
    } catch (MalformedJwtException ex) {
      log.error("Invalid JWT token");
    } catch (ExpiredJwtException ex) {
      log.error("Expired JWT token");
    } catch (UnsupportedJwtException ex) {
      log.error("Unsupported JWT token");
    } catch (IllegalArgumentException ex) {
      log.error("JWT claims string is empty");
    }
    return false;
  }
}
