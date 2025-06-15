package com.payflow.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI documentation. Sets up Swagger/OpenAPI documentation for the PayFlow
 * API.
 */
@Configuration
public class OpenApiConfig {

  /** Private constructor to prevent instantiation. */
  private OpenApiConfig() {
    // Utility class
  }

  @Bean
  public OpenAPI customOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("PayFlow Lite API")
                .description("RESTful API for the PayFlow Lite digital wallet system")
                .version("1.0.0")
                .contact(
                    new Contact()
                        .name("PayFlow Team")
                        .email("support@payflow.example.com")
                        .url("https://payflow.example.com"))
                .license(
                    new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .name("bearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter JWT Bearer token")));
  }
}
