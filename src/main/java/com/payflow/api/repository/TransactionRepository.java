package com.payflow.api.repository;

import com.payflow.api.model.entity.Transaction;
import com.payflow.api.model.entity.User;
import com.payflow.api.model.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionNumber(String transactionNumber);

    @Query(value = "SELECT t FROM Transaction t JOIN FETCH t.sender JOIN FETCH t.receiver WHERE t.sender = :user OR t.receiver = :user ORDER BY t.createdAt DESC", countQuery = "SELECT count(t) FROM Transaction t WHERE t.sender = :user OR t.receiver = :user")
    Page<Transaction> findBySenderOrReceiverOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    @Query(value = "SELECT t FROM Transaction t JOIN FETCH t.sender JOIN FETCH t.receiver JOIN FETCH t.sourceWallet JOIN FETCH t.destinationWallet WHERE t.sourceWallet = :wallet OR t.destinationWallet = :wallet ORDER BY t.createdAt DESC", countQuery = "SELECT count(t) FROM Transaction t WHERE t.sourceWallet = :wallet OR t.destinationWallet = :wallet")
    Page<Transaction> findByWalletOrderByCreatedAtDesc(@Param("wallet") Wallet wallet, Pageable pageable);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.sender JOIN FETCH t.receiver WHERE (t.sender = :user OR t.receiver = :user) AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByUserAndDateRange(@Param("user") User user, @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
