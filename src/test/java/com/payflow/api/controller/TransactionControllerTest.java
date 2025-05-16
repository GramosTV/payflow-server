package com.payflow.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.api.model.dto.request.TransactionRequest;
import com.payflow.api.model.dto.response.TransactionResponse;
import com.payflow.api.model.entity.Transaction;
import com.payflow.api.model.entity.User;
import com.payflow.api.model.entity.Wallet;
import com.payflow.api.security.JwtTokenProvider;
import com.payflow.api.security.UserPrincipal;
import com.payflow.api.service.TransactionService;
import com.payflow.api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private User testUser;
    private Transaction testTransaction;
    private TransactionRequest transactionRequest;
    private UserPrincipal userPrincipal;
    private String jwtToken;

    @BeforeEach
    public void setup() {
        // Initialize test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");

        // Create source wallet
        Wallet sourceWallet = new Wallet();
        sourceWallet.setId(1L);
        sourceWallet.setUser(testUser);
        sourceWallet.setCurrency(Wallet.Currency.USD);
        sourceWallet.setBalance(BigDecimal.valueOf(1000));
        sourceWallet.setWalletNumber("WALLET123456");

        // Create destination wallet
        User recipient = new User();
        recipient.setId(2L);
        recipient.setEmail("recipient@example.com");

        Wallet destWallet = new Wallet();
        destWallet.setId(2L);
        destWallet.setUser(recipient);
        destWallet.setCurrency(Wallet.Currency.USD);
        destWallet.setBalance(BigDecimal.valueOf(500));
        destWallet.setWalletNumber("WALLET654321");

        // Initialize test transaction
        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setTransactionNumber("TXN123456");
        testTransaction.setSender(testUser);
        testTransaction.setReceiver(recipient);
        testTransaction.setSourceWallet(sourceWallet);
        testTransaction.setDestinationWallet(destWallet);
        testTransaction.setAmount(BigDecimal.valueOf(100));
        testTransaction.setType(Transaction.TransactionType.TRANSFER);
        testTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        testTransaction.setSourceCurrency(Wallet.Currency.USD);
        testTransaction.setDestinationCurrency(Wallet.Currency.USD);
        testTransaction.setCreatedAt(Instant.now());

        // Initialize transaction request
        transactionRequest = new TransactionRequest();
        transactionRequest.setSourceWalletNumber("WALLET123456");
        transactionRequest.setDestinationWalletNumber("WALLET654321");
        transactionRequest.setAmount(BigDecimal.valueOf(100));
        transactionRequest.setDescription("Test transfer");

        // Initialize user principal
        userPrincipal = new UserPrincipal(
                testUser.getId(),
                testUser.getEmail(),
                "hashed_password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        // Create JWT token
        jwtToken = "Bearer test-jwt-token";

        // Mock JWT authentication
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJWT(anyString())).thenReturn(testUser.getId());
    }

    @Test
    public void testTransferMoney() throws Exception {
        when(userService.getUserById(anyLong())).thenReturn(testUser);
        when(transactionService.createTransferTransaction(any(User.class), any(TransactionRequest.class)))
                .thenReturn(testTransaction);

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionNumber").value("TXN123456"))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    public void testGetMyTransactions() throws Exception {
        Page<Transaction> transactionPage = new PageImpl<>(
                Collections.singletonList(testTransaction),
                PageRequest.of(0, 10), 1);

        when(userService.getUserById(anyLong())).thenReturn(testUser);
        when(transactionService.getUserTransactions(any(User.class), any(Pageable.class)))
                .thenReturn(transactionPage);

        mockMvc.perform(get("/transactions")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].transactionNumber").value("TXN123456"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    public void testGetTransactionById() throws Exception {
        when(transactionService.getTransactionById(anyLong())).thenReturn(testTransaction);

        mockMvc.perform(get("/transactions/1")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionNumber").value("TXN123456"));
    }

    @Test
    public void testSearchTransactions() throws Exception {
        List<Transaction> transactions = Arrays.asList(testTransaction);

        when(userService.getUserById(anyLong())).thenReturn(testUser);
        when(transactionService.getUserTransactionsInDateRange(
                any(User.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(transactions);

        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        mockMvc.perform(get("/transactions/search")
                .header("Authorization", jwtToken)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].transactionNumber").value("TXN123456"));
    }

    @Test
    public void testTransferMoney_InvalidData() throws Exception {
        // Create invalid request with zero amount
        TransactionRequest invalidRequest = new TransactionRequest();
        invalidRequest.setSourceWalletNumber("WALLET123456");
        invalidRequest.setDestinationWalletNumber("WALLET654321");
        invalidRequest.setAmount(BigDecimal.ZERO); // Invalid amount

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
