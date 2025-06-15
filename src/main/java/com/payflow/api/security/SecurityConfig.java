package com.payflow.api.security;

import java.util.Arrays;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security configuration for the PayFlow API. Configures JWT-based authentication, CORS, CSRF,
 * security headers, and access controls.
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

  private final JwtAuthenticationEntryPoint unauthorizedHandler;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
      throws Exception {
    return authConfig.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12); // Increased strength to 12 from default 10
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    log.debug("Configuring Spring Security");

    http
        // CORS configuration
        .cors()
        .configurationSource(corsConfigurationSource())
        .and()

        // CSRF - disabled for REST API with JWT auth
        .csrf()
        .disable()

        // Exception handling for unauthorized requests
        .exceptionHandling()
        .authenticationEntryPoint(unauthorizedHandler)
        .and()

        // Use stateless session management with JWT
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()

        // Security headers
        .headers()
        .contentSecurityPolicy(
            "default-src 'self'; frame-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self' data:; connect-src 'self'")
        .and()
        .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
        .and()
        .permissionsPolicy()
        .policy(
            "camera=(), microphone=(), geolocation=(), payment=(), usb=(), magnetometer=(), accelerometer=(), gyroscope=()")
        .and()
        .frameOptions()
        .deny()
        .and()

        // Request authorization rules
        .authorizeRequests()
        // Public endpoints
        .antMatchers("/auth/**")
        .permitAll()
        .antMatchers(HttpMethod.OPTIONS, "/**")
        .permitAll()

        // OpenAPI documentation
        .antMatchers("/swagger-ui/**", "/v3/api-docs/**")
        .permitAll()

        // Development endpoints (disable in production)
        .antMatchers("/h2-console/**")
        .permitAll()

        // Monitoring endpoints
        .antMatchers("/actuator/health")
        .permitAll()
        .antMatchers("/actuator/info")
        .permitAll()
        .antMatchers("/actuator/**")
        .hasRole("ADMIN")

        // Public resources
        .antMatchers("/public/**")
        .permitAll()

        // All other endpoints require authentication
        .anyRequest()
        .authenticated();

    // Add JWT authentication filter
    http.addFilterBefore(
        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // Disable
    // frameOptions for
    // H2 console
    // (development only
    // - remove in
    // production)
    if (isDevelopmentEnvironment()) {
      log.warn("H2 console access enabled - REMOVE IN PRODUCTION");
      http.headers().frameOptions().disable();
    }

    return http.build();
  }

  /**
   * Check if application is running in development mode
   *
   * @return true if in development environment
   */
  private boolean isDevelopmentEnvironment() {
    String env = System.getProperty("spring.profiles.active");
    return env != null && (env.contains("dev") || env.contains("local"));
  }

  /**
   * Configure CORS for the application
   *
   * @return CORS configuration
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // In production, replace with specific origins
    configuration.setAllowedOriginPatterns(Collections.singletonList("*"));

    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Auth-Token",
            "X-Requested-With",
            "Origin",
            "Accept",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"));
    configuration.setExposedHeaders(Arrays.asList("X-Auth-Token", "Authorization"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L); // 1 hour

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }
}
