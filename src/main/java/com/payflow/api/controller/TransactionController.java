package com.payflow.api.controller;

import com.payflow.api.model.dto.request.TransactionRequest;
import com.payflow.api.model.dto.response.TransactionResponse;
import com.payflow.api.model.entity.Transaction;
import com.payflow.api.model.entity.User;
import com.payflow.api.security.UserPrincipal;
import com.payflow.api.service.TransactionService;
import com.payflow.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction management API")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;

    @PostMapping("/transfer")
    @Operation(summary = "Transfer money between wallets")
    public ResponseEntity<TransactionResponse> transferMoney(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody TransactionRequest transactionRequest) {

        User user = userService.getUserById(currentUser.getId());
        Transaction transaction = transactionService.createTransferTransaction(user, transactionRequest);

        return new ResponseEntity<>(TransactionResponse.fromEntity(transaction), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all transactions for the current user")
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {

        User user = userService.getUserById(currentUser.getId());
        Page<Transaction> transactions = transactionService.getUserTransactions(user, pageable);

        Page<TransactionResponse> transactionResponses = transactions.map(TransactionResponse::fromEntity);

        return ResponseEntity.ok(transactionResponses);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get a transaction by ID")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long transactionId) {

        Transaction transaction = transactionService.getTransactionById(transactionId);

        // Check if the transaction involves the current user
        if (!transaction.getSender().getId().equals(currentUser.getId()) &&
                !transaction.getReceiver().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(TransactionResponse.fromEntity(transaction));
    }

    @GetMapping("/search")
    @Operation(summary = "Search transactions by date range")
    public ResponseEntity<List<TransactionResponse>> searchTransactions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        User user = userService.getUserById(currentUser.getId());

        LocalDateTime startOfDay = startDate.atStartOfDay();
        LocalDateTime endOfDay = endDate.atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionService.getUserTransactionsInDateRange(
                user, startOfDay, endOfDay);

        List<TransactionResponse> transactionResponses = transactions.stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(transactionResponses);
    }
}
