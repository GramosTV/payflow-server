package com.payflow.api.repository;

import com.payflow.api.model.entity.Transaction;
import com.payflow.api.model.entity.User;
import com.payflow.api.model.entity.Wallet;
import com.payflow.api.repository.projection.TransactionSummary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Transaction entity operations. Provides custom queries for transaction
 * management and retrieval.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  String ORDER_BY_CREATED_AT_DESC = "ORDER BY t.createdAt DESC";

  Optional<Transaction> findByTransactionNumber(String transactionNumber);

  @Query(
      value =
          "SELECT t FROM Transaction t "
              + "JOIN FETCH t.sender s "
              + "JOIN FETCH t.receiver r "
              + "JOIN FETCH t.sourceWallet sw "
              + "JOIN FETCH t.destinationWallet dw "
              + "WHERE t.sender = :user OR t.receiver = :user "
              + ORDER_BY_CREATED_AT_DESC,
      countQuery =
          "SELECT count(t) FROM Transaction t WHERE t.sender = :user OR t.receiver = :user")
  Page<Transaction> findBySenderOrReceiverOrderByCreatedAtDesc(
      @Param("user") User user, Pageable pageable);

  @Query(
      value =
          "SELECT t FROM Transaction t "
              + "JOIN FETCH t.sender s "
              + "JOIN FETCH t.receiver r "
              + "JOIN FETCH t.sourceWallet sw "
              + "JOIN FETCH t.destinationWallet dw "
              + "WHERE t.sourceWallet = :wallet OR t.destinationWallet = :wallet "
              + ORDER_BY_CREATED_AT_DESC,
      countQuery =
          "SELECT count(t) FROM Transaction t WHERE t.sourceWallet = :wallet OR t.destinationWallet = :wallet")
  Page<Transaction> findByWalletOrderByCreatedAtDesc(
      @Param("wallet") Wallet wallet, Pageable pageable);

  @Query(
      "SELECT t FROM Transaction t "
          + "JOIN FETCH t.sender s "
          + "JOIN FETCH t.receiver r "
          + "JOIN FETCH t.sourceWallet sw "
          + "JOIN FETCH t.destinationWallet dw "
          + "WHERE (t.sender = :user OR t.receiver = :user) "
          + "AND t.createdAt BETWEEN :startDate AND :endDate "
          + ORDER_BY_CREATED_AT_DESC)
  List<Transaction> findByUserAndDateRange(
      @Param("user") User user,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  @Query(
      "SELECT t.id as id, "
          + "t.transactionNumber as transactionNumber, "
          + "t.amount as amount, "
          + "t.type as type, "
          + "t.status as status, "
          + "t.createdAt as createdAt, "
          + "s.fullName as senderName, "
          + "r.fullName as receiverName "
          + "FROM Transaction t "
          + "JOIN t.sender s "
          + "JOIN t.receiver r "
          + "WHERE t.sender.id = :userId OR t.receiver.id = :userId "
          + ORDER_BY_CREATED_AT_DESC)
  Page<TransactionSummary> findSummariesByUserId(@Param("userId") Long userId, Pageable pageable);
}
