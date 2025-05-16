package com.payflow.api.repository;

import com.payflow.api.model.entity.Transaction;
import com.payflow.api.model.entity.User;
import com.payflow.api.model.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionNumber(String transactionNumber);

    Page<Transaction> findBySenderOrReceiverOrderByCreatedAtDesc(User sender, User receiver, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.sourceWallet = ?1 OR t.destinationWallet = ?1 ORDER BY t.createdAt DESC")
    Page<Transaction> findByWalletOrderByCreatedAtDesc(Wallet wallet, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.sender = ?1 OR t.receiver = ?1) AND t.createdAt BETWEEN ?2 AND ?3 ORDER BY t.createdAt DESC")
    List<Transaction> findByUserAndDateRange(User user, LocalDateTime startDate, LocalDateTime endDate);
}
