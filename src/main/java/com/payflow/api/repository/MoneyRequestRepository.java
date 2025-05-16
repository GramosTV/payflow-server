package com.payflow.api.repository;

import com.payflow.api.model.entity.MoneyRequest;
import com.payflow.api.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoneyRequestRepository extends JpaRepository<MoneyRequest, Long> {
    Optional<MoneyRequest> findByRequestNumber(String requestNumber);

    @Query("SELECT m FROM MoneyRequest m WHERE m.requester = ?1 ORDER BY m.createdAt DESC")
    Page<MoneyRequest> findByRequesterOrderByCreatedAtDesc(User requester, Pageable pageable);

    @Query("SELECT m FROM MoneyRequest m WHERE m.requestee = ?1 ORDER BY m.createdAt DESC")
    Page<MoneyRequest> findByRequesteeOrderByCreatedAtDesc(User requestee, Pageable pageable);

    @Query("SELECT m FROM MoneyRequest m WHERE m.status = 'PENDING' AND m.expiresAt < ?1")
    List<MoneyRequest> findExpiredRequests(LocalDateTime currentTime);

    @Query("SELECT m FROM MoneyRequest m WHERE m.requestee = ?1 AND m.status = 'PENDING' ORDER BY m.createdAt DESC")
    List<MoneyRequest> findPendingRequestsForUser(User user);
}
