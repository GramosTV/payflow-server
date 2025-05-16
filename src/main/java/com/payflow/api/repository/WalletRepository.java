package com.payflow.api.repository;

import com.payflow.api.model.entity.User;
import com.payflow.api.model.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    List<Wallet> findByUser(User user);

    List<Wallet> findByUserOrderByCreatedAtDesc(User user);

    Optional<Wallet> findByUserAndCurrency(User user, Wallet.Currency currency);

    Optional<Wallet> findByWalletNumber(String walletNumber);
}
