package com.payflow.api.repository;

import com.payflow.api.model.entity.ExchangeRate;
import com.payflow.api.model.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findByBaseCurrencyAndTargetCurrency(Wallet.Currency baseCurrency,
            Wallet.Currency targetCurrency);
}
