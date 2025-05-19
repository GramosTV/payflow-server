package com.payflow.api.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * Configuration class that runs Flyway migrations manually after the
 * application context
 * is fully initialized. This avoids circular dependency issues during startup.
 */
@Configuration
@Order(Ordered.LOWEST_PRECEDENCE)
public class ManualFlywayMigration implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ManualFlywayMigration.class);
    private final DataSource dataSource;
    private final Environment environment;

    public ManualFlywayMigration(DataSource dataSource, Environment environment) {
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(@org.springframework.lang.NonNull ApplicationReadyEvent event) {
        logger.info("Running Flyway migrations manually after application startup");

        String[] activeProfiles = environment.getActiveProfiles();
        logger.info("Active profiles: {}", Arrays.toString(activeProfiles));

        // Determine if we're using H2 or PostgreSQL
        boolean isDevProfile = Arrays.stream(activeProfiles).anyMatch(p -> p.equalsIgnoreCase("dev"));
        String dbType = isDevProfile ? "h2" : "postgres";
        logger.info("Using database type: {}", dbType);

        // Configure Flyway with the appropriate migration path
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .locations("classpath:db/migration/" + dbType)
                .load();

        // Execute the migration
        try {
            flyway.migrate();
            logger.info("Flyway migrations completed successfully");
        } catch (Exception e) {
            logger.error("Error executing Flyway migrations", e);
        }
    }
}
