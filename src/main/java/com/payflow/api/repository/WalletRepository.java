package com.payflow.api.repository;

import com.payflow.api.model.entity.User;
import com.payflow.api.model.entity.Wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Wallet entity operations
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    List<Wallet> findByUser(User user);

    List<Wallet> findByUserOrderByCreatedAtDesc(User user);

    Optional<Wallet> findByUserAndCurrency(User user, Wallet.Currency currency);

    Optional<Wallet> findByWalletNumber(String walletNumber);

    /**
     * Find a wallet by its wallet number with a pessimistic write lock
     * This ensures exclusive access when updating the wallet balance
     * 
     * @param walletNumber The wallet number to search for
     * @return Optional wallet with a lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.walletNumber = :walletNumber")
    Optional<Wallet> findByWalletNumberWithLock(@Param("walletNumber") String walletNumber);
}
