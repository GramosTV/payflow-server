package com.payflow.api.repository;

import com.payflow.api.model.entity.PaymentMethod;
import com.payflow.api.model.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

  List<PaymentMethod> findByUser(User user);

  List<PaymentMethod> findByUserOrderByCreatedAtDesc(User user);
}
