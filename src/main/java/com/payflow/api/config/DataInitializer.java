package com.payflow.api.config;

import com.payflow.api.model.entity.User;
import com.payflow.api.model.entity.Wallet;
import com.payflow.api.repository.UserRepository;
import com.payflow.api.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final PasswordEncoder passwordEncoder;

    @Bean
    @Profile("!prod")
    public CommandLineRunner initData(UserRepository userRepository, WalletService walletService) {
        return args -> {
            log.info("Initializing demo data...");

            // Create admin user
            if (!userRepository.existsByEmail("admin@payflow.com")) {
                User admin = new User();
                admin.setEmail("admin@payflow.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setFullName("Admin User");
                admin.setRole(User.UserRole.ADMIN);
                admin.setEnabled(true);
                userRepository.save(admin);

                // Create wallets for admin
                createWallets(admin, walletService);
            }

            // Create regular demo user
            if (!userRepository.existsByEmail("user@payflow.com")) {
                User user = new User();
                user.setEmail("user@payflow.com");
                user.setPassword(passwordEncoder.encode("user123"));
                user.setFullName("Demo User");
                user.setPhoneNumber("+1234567890");
                user.setRole(User.UserRole.USER);
                user.setEnabled(true);
                userRepository.save(user);

                // Create wallets for demo user
                createWallets(user, walletService);
            }

            log.info("Demo data initialized successfully");
        };
    }

    private void createWallets(User user, WalletService walletService) {
        // Create USD wallet with initial balance
        Wallet usdWallet = walletService.createWallet(user, Wallet.Currency.USD, new BigDecimal("1000.00"));
        log.info("Created USD wallet for {}: {}", user.getEmail(), usdWallet.getWalletNumber());

        // Create EUR wallet with initial balance
        Wallet eurWallet = walletService.createWallet(user, Wallet.Currency.EUR, new BigDecimal("500.00"));
        log.info("Created EUR wallet for {}: {}", user.getEmail(), eurWallet.getWalletNumber());
    }
}
