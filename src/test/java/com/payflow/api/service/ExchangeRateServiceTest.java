package com.payflow.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.payflow.api.exception.ResourceNotFoundException;
import com.payflow.api.model.entity.ExchangeRate;
import com.payflow.api.model.entity.Wallet;
import com.payflow.api.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class ExchangeRateServiceTest {

  @Mock private ExchangeRateRepository exchangeRateRepository;

  @Mock private RestTemplate restTemplate;

  @InjectMocks private ExchangeRateService exchangeRateService;

  private ExchangeRate usdToEur;
  private ExchangeRate usdToGbp;

  @BeforeEach
  public void setup() {
    ReflectionTestUtils.setField(
        exchangeRateService, "exchangeRateApiUrl", "https://api.example.com/latest/");

    // Set up exchange rates
    usdToEur = new ExchangeRate();
    usdToEur.setBaseCurrency(Wallet.Currency.USD);
    usdToEur.setTargetCurrency(Wallet.Currency.EUR);
    usdToEur.setRate(new BigDecimal("0.85"));

    usdToGbp = new ExchangeRate();
    usdToGbp.setBaseCurrency(Wallet.Currency.USD);
    usdToGbp.setTargetCurrency(Wallet.Currency.GBP);
    usdToGbp.setRate(new BigDecimal("0.75"));
  }

  @Test
  public void testGetExchangeRate_SameCurrency() {
    // Test exchange rate for same currency
    BigDecimal rate = exchangeRateService.getExchangeRate(Wallet.Currency.USD, Wallet.Currency.USD);
    assertEquals(BigDecimal.ONE, rate);

    verify(exchangeRateRepository, never()).findByBaseCurrencyAndTargetCurrency(any(), any());
  }

  @Test
  public void testGetExchangeRate_DifferentCurrency() {
    // Arrange
    when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency(
            eq(Wallet.Currency.USD), eq(Wallet.Currency.EUR)))
        .thenReturn(Optional.of(usdToEur));

    // Act
    BigDecimal rate = exchangeRateService.getExchangeRate(Wallet.Currency.USD, Wallet.Currency.EUR);

    // Assert
    assertEquals(new BigDecimal("0.85"), rate);

    // Verify
    verify(exchangeRateRepository)
        .findByBaseCurrencyAndTargetCurrency(Wallet.Currency.USD, Wallet.Currency.EUR);
  }

  @Test
  public void testGetExchangeRate_NotFound() {
    // Arrange
    when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency(
            eq(Wallet.Currency.USD), eq(Wallet.Currency.JPY)))
        .thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(
        ResourceNotFoundException.class,
        () -> exchangeRateService.getExchangeRate(Wallet.Currency.USD, Wallet.Currency.JPY));

    // Verify
    verify(exchangeRateRepository)
        .findByBaseCurrencyAndTargetCurrency(Wallet.Currency.USD, Wallet.Currency.JPY);
  }

  @Test
  public void testConvertCurrency() {
    // Arrange
    when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency(
            eq(Wallet.Currency.USD), eq(Wallet.Currency.EUR)))
        .thenReturn(Optional.of(usdToEur));

    BigDecimal amount = new BigDecimal("100");

    // Act
    BigDecimal convertedAmount =
        exchangeRateService.convertCurrency(amount, Wallet.Currency.USD, Wallet.Currency.EUR);

    // Assert
    assertEquals(new BigDecimal("85.0000"), convertedAmount);

    // Verify
    verify(exchangeRateRepository)
        .findByBaseCurrencyAndTargetCurrency(Wallet.Currency.USD, Wallet.Currency.EUR);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testUpdateAllExchangeRates() throws Exception {
    // Mocked API response
    Map<String, Double> rates = new HashMap<>();
    rates.put("USD", 1.0);
    rates.put("EUR", 0.85);
    rates.put("GBP", 0.75);

    // Create response object using reflection to access private class
    Object rateResponse =
        Class.forName("com.payflow.api.service.ExchangeRateService$ExchangeRateApiResponse")
            .getDeclaredConstructor()
            .newInstance();
    ReflectionTestUtils.setField(rateResponse, "base", "USD");
    ReflectionTestUtils.setField(rateResponse, "rates", rates);

    // Mock REST API call
    ResponseEntity<Object> responseEntity = new ResponseEntity<>(rateResponse, HttpStatus.OK);
    when(restTemplate.getForEntity(anyString(), eq(Object.class)))
        .thenReturn((ResponseEntity) responseEntity);

    // When existing exchange rate
    when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency(
            any(Wallet.Currency.class), any(Wallet.Currency.class)))
        .thenReturn(Optional.of(usdToEur));

    // Call method
    exchangeRateService.updateAllExchangeRates();

    // Difficult to test private methods directly, so verify interactions
    verify(restTemplate, atLeastOnce()).getForEntity(contains("USD"), any());
    verify(exchangeRateRepository, atLeastOnce()).save(any(ExchangeRate.class));
  }
}
