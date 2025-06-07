package com.payflow.api.controller;

import com.payflow.api.exception.BadRequestException;
import com.payflow.api.exception.ResourceNotFoundException;
import com.payflow.api.model.dto.request.TransactionRequest;
import com.payflow.api.model.dto.response.TransactionResponse;
import com.payflow.api.model.entity.Transaction;
import com.payflow.api.model.entity.User;
import com.payflow.api.repository.projection.TransactionSummary;
import com.payflow.api.security.UserPrincipal;
import com.payflow.api.service.TransactionService;
import com.payflow.api.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

/**
 * REST controller for managing transactions
 */
@RestController
@RequestMapping("transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction management API")
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;

    @PostMapping("/transfer")
    @Operation(summary = "Transfer money between wallets", description = "Transfer funds from one wallet to another. The source wallet must belong to the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transfer successful", content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data or insufficient funds"),
            @ApiResponse(responseCode = "403", description = "User does not own the source wallet"),
            @ApiResponse(responseCode = "404", description = "Wallet not found")
    })
    public ResponseEntity<TransactionResponse> transferMoney(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody TransactionRequest transactionRequest) {

        log.info("Transfer request received from user {} for amount {}",
                currentUser.getId(), transactionRequest.getAmount());

        User user = userService.getUserById(currentUser.getId());
        Transaction transaction = transactionService.createTransferTransaction(user, transactionRequest);

        log.info("Transfer completed successfully, transaction ID: {}", transaction.getId());

        return new ResponseEntity<>(TransactionResponse.fromEntity(transaction), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all transactions for the current user", description = "Retrieves a paginated list of all transactions where the current user is either sender or receiver")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated")
    })
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {

        log.debug("Fetching transactions for user {}, page {}, size {}",
                currentUser.getId(), pageable.getPageNumber(), pageable.getPageSize());

        User user = userService.getUserById(currentUser.getId());
        Page<Transaction> transactions = transactionService.getUserTransactions(user, pageable);

        Page<TransactionResponse> transactionResponses = transactions.map(TransactionResponse::fromEntity);

        log.debug("Returned {} transactions out of {}",
                transactionResponses.getNumberOfElements(), transactionResponses.getTotalElements());

        return ResponseEntity.ok(transactionResponses);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get transaction summaries for the current user", description = "Retrieves a paginated list of transaction summaries with optimized data loading")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction summaries retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated")
    })
    public ResponseEntity<Page<TransactionSummary>> getTransactionSummaries(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {

        log.debug("Fetching transaction summaries for user {}, page {}, size {}",
                currentUser.getId(), pageable.getPageNumber(), pageable.getPageSize());

        User user = userService.getUserById(currentUser.getId());
        Page<TransactionSummary> summaries = transactionService.getUserTransactionSummaries(user, pageable);

        log.debug("Returned {} transaction summaries out of {}",
                summaries.getNumberOfElements(), summaries.getTotalElements());

        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get a transaction by ID", description = "Retrieves detailed information about a specific transaction. User must be either the sender or receiver.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "User is not authorized to view this transaction"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<TransactionResponse> getTransactionById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable @Parameter(description = "ID of the transaction") Long transactionId) {

        log.debug("Fetching transaction {} for user {}", transactionId, currentUser.getId());

        Transaction transaction = transactionService.getTransactionById(transactionId);
        if (!transaction.getSender().getId().equals(currentUser.getId()) &&
                !transaction.getReceiver().getId().equals(currentUser.getId())) {
            log.warn("User {} attempted to access transaction {} that they're not involved in",
                    currentUser.getId(), transactionId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.debug("Transaction {} successfully retrieved", transactionId);
        return ResponseEntity.ok(TransactionResponse.fromEntity(transaction));
    }

    @GetMapping("/search")
    @Operation(summary = "Search transactions by date range", description = "Retrieves all transactions for the current user within the specified date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date range parameters")
    })
    public ResponseEntity<List<TransactionResponse>> searchTransactions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "Start date in ISO format (YYYY-MM-DD)") LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "End date in ISO format (YYYY-MM-DD)") LocalDate endDate) {

        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("End date must be after start date");
        }

        log.debug("Searching transactions for user {} between {} and {}",
                currentUser.getId(), startDate, endDate);

        User user = userService.getUserById(currentUser.getId());

        LocalDateTime startOfDay = startDate.atStartOfDay();
        LocalDateTime endOfDay = endDate.atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionService.getUserTransactionsInDateRange(
                user, startOfDay, endOfDay);

        List<TransactionResponse> transactionResponses = transactions.stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());

        log.debug("Found {} transactions in date range", transactionResponses.size());
        return ResponseEntity.ok(transactionResponses);
    }
}
