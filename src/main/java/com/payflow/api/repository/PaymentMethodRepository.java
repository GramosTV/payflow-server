package com.payflow.api.repository;

import com.payflow.api.model.entity.PaymentMethod;
import com.payflow.api.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByUser(User user);

    List<PaymentMethod> findByUserOrderByCreatedAtDesc(User user);
}
