package com.payflow.api.service;

import com.payflow.api.exception.ResourceNotFoundException;
import com.payflow.api.model.entity.ExchangeRate;
import com.payflow.api.model.entity.Wallet;
import com.payflow.api.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/** Service class for managing exchange rates between currencies. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

  private final ExchangeRateRepository exchangeRateRepository;
  private final RestTemplate restTemplate;

  @Value("${exchange.rate.api.url}")
  private String exchangeRateApiUrl;

  /** Initializes exchange rates on service startup. */
  @PostConstruct
  public void initializeExchangeRates() {
    updateAllExchangeRates();
  }

  /** Scheduled task to update all exchange rates. */
  @Scheduled(cron = "${exchange.rate.update.schedule}")
  public void updateAllExchangeRates() {
    log.info("Updating exchange rates");

    try {
      for (final Wallet.Currency baseCurrency : Wallet.Currency.values()) {
        updateExchangeRatesForCurrency(baseCurrency);
      }
      log.info("Exchange rates updated successfully");
    } catch (final Exception e) {
      log.error("Failed to update exchange rates: {}", e.getMessage(), e);
    }
  }

  private void updateExchangeRatesForCurrency(final Wallet.Currency baseCurrency) {
    try {
      final String url = exchangeRateApiUrl + baseCurrency.name();
      final ResponseEntity<ExchangeRateApiResponse> response =
          restTemplate.getForEntity(url, ExchangeRateApiResponse.class);

      if (response.getBody() != null && response.getBody().getRates() != null) {
        final Map<String, Double> rates = response.getBody().getRates();

        for (final Wallet.Currency targetCurrency : Wallet.Currency.values()) {
          if (rates.containsKey(targetCurrency.name())) {
            final Double rateValue = rates.get(targetCurrency.name());
            if (rateValue != null) {
              final BigDecimal rate = BigDecimal.valueOf(rateValue);
              saveOrUpdateExchangeRate(baseCurrency, targetCurrency, rate);
            }
          }
        }
      }
    } catch (final Exception e) {
      log.error("Failed to update exchange rates for {}: {}", baseCurrency, e.getMessage(), e);
    }
  }

  private void saveOrUpdateExchangeRate(
      final Wallet.Currency baseCurrency,
      final Wallet.Currency targetCurrency,
      final BigDecimal rate) {
    exchangeRateRepository
        .findByBaseCurrencyAndTargetCurrency(baseCurrency, targetCurrency)
        .ifPresentOrElse(
            exchangeRate -> {
              exchangeRate.setRate(rate);
              exchangeRateRepository.save(exchangeRate);
            },
            () ->
                exchangeRateRepository.save(new ExchangeRate(baseCurrency, targetCurrency, rate)));
  }

  /**
   * Retrieves the exchange rate between two currencies.
   *
   * @param fromCurrency source currency
   * @param toCurrency target currency
   * @return exchange rate
   * @throws ResourceNotFoundException if exchange rate not found
   */
  public BigDecimal getExchangeRate(
      final Wallet.Currency fromCurrency, final Wallet.Currency toCurrency) {
    if (fromCurrency == toCurrency) {
      return BigDecimal.ONE;
    }

    return exchangeRateRepository
        .findByBaseCurrencyAndTargetCurrency(fromCurrency, toCurrency)
        .map(ExchangeRate::getRate)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "ExchangeRate", "currencies", fromCurrency + " to " + toCurrency));
  }

  /**
   * Converts an amount from one currency to another.
   *
   * @param amount the amount to convert
   * @param fromCurrency source currency
   * @param toCurrency target currency
   * @return converted amount
   */
  public BigDecimal convertCurrency(
      final BigDecimal amount,
      final Wallet.Currency fromCurrency,
      final Wallet.Currency toCurrency) {
    final BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
    return amount.multiply(rate).setScale(4, RoundingMode.HALF_UP);
  }

  /** DTO for the exchange rate API response. */
  private static class ExchangeRateApiResponse {
    private String base;
    private Map<String, Double> rates;

    public String getBase() {
      return base;
    }

    public void setBase(final String base) {
      this.base = base;
    }

    public Map<String, Double> getRates() {
      return rates;
    }

    public void setRates(final Map<String, Double> rates) {
      this.rates = rates;
    }
  }
}
