package com.payflow.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = FlywayAutoConfiguration.class) // Re-added the exclusion
@EnableScheduling
public class PayflowApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayflowApiApplication.class, args);
    }
}
