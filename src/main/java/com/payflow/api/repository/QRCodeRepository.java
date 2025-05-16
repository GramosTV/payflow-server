package com.payflow.api.repository;

import com.payflow.api.model.entity.QRCode;
import com.payflow.api.model.entity.User;
import com.payflow.api.model.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QRCodeRepository extends JpaRepository<QRCode, Long> {
    Optional<QRCode> findByQrId(String qrId);

    @Query("SELECT q FROM QRCode q WHERE q.wallet.user = ?1 ORDER BY q.createdAt DESC")
    List<QRCode> findByUser(User user);

    List<QRCode> findByWallet(Wallet wallet);

    @Query("SELECT q FROM QRCode q WHERE q.isActive = true AND (q.expiresAt IS NULL OR q.expiresAt > CURRENT_TIMESTAMP)")
    List<QRCode> findAllActiveQRCodes();
}
