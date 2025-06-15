package com.payflow.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** Application configuration class. Contains bean definitions for application-wide components. */
@Configuration
public class AppConfig {

  /** Private constructor to prevent instantiation. */
  private AppConfig() {
    // Utility class
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
