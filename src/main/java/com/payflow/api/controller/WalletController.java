package com.payflow.api.controller;

import com.payflow.api.model.dto.request.TopUpRequest;
import com.payflow.api.model.dto.request.WalletRequest;
import com.payflow.api.model.dto.request.DepositRequest;
import com.payflow.api.model.dto.request.WithdrawRequest;
import com.payflow.api.model.dto.response.TransactionResponse;
import com.payflow.api.model.dto.response.WalletResponse;
import com.payflow.api.model.entity.Transaction;
import com.payflow.api.model.entity.User;
import com.payflow.api.model.entity.Wallet;
import com.payflow.api.security.UserPrincipal;
import com.payflow.api.service.TransactionService;
import com.payflow.api.service.UserService;
import com.payflow.api.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets", description = "Wallet management API")
public class WalletController {

    private final WalletService walletService;
    private final UserService userService;
    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Create a new wallet")
    public ResponseEntity<WalletResponse> createWallet(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody WalletRequest walletRequest) {

        User user = userService.getUserById(currentUser.getId());
        Wallet wallet = walletService.createWallet(user, walletRequest);

        return new ResponseEntity<>(WalletResponse.fromEntity(wallet), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all wallets for the current user")
    public ResponseEntity<List<WalletResponse>> getMyWallets(@AuthenticationPrincipal UserPrincipal currentUser) {
        User user = userService.getUserById(currentUser.getId());
        List<Wallet> wallets = walletService.getUserWallets(user);

        List<WalletResponse> walletResponses = wallets.stream()
                .map(WalletResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(walletResponses);
    }

    @GetMapping("/primary")
    @Operation(summary = "Get the user's primary wallet")
    public ResponseEntity<WalletResponse> getPrimaryWallet(@AuthenticationPrincipal UserPrincipal currentUser) {
        User user = userService.getUserById(currentUser.getId());
        List<Wallet> wallets = walletService.getUserWallets(user);

        // Default to first wallet if available
        if (wallets.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // For now, we'll consider the first wallet (usually USD) as primary
        Wallet primaryWallet = wallets.get(0);

        return ResponseEntity.ok(WalletResponse.fromEntity(primaryWallet));
    }

    @GetMapping("/{walletId}")
    @Operation(summary = "Get a wallet by ID")
    public ResponseEntity<WalletResponse> getWalletById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long walletId) {

        Wallet wallet = walletService.getWalletById(walletId);

        // Check if the wallet belongs to the current user
        if (!wallet.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(WalletResponse.fromEntity(wallet));
    }

    @PostMapping("/topup")
    @Operation(summary = "Top up (add funds to) a wallet")
    public ResponseEntity<TransactionResponse> topUpWallet(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody TopUpRequest topUpRequest) {

        User user = userService.getUserById(currentUser.getId());
        Transaction transaction = walletService.topUpWallet(user, topUpRequest);

        return new ResponseEntity<>(TransactionResponse.fromEntity(transaction), HttpStatus.CREATED);
    }

    @PostMapping("/deposit")
    @Operation(summary = "Deposit funds into a wallet from a payment method")
    public ResponseEntity<WalletResponse> depositFunds(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody DepositRequest depositRequest) {

        User user = userService.getUserById(currentUser.getId());
        Wallet wallet = walletService.depositFunds(user, depositRequest.getAmount(),
                depositRequest.getPaymentMethodId());

        return new ResponseEntity<>(WalletResponse.fromEntity(wallet), HttpStatus.OK);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw funds from a wallet to a payment method")
    public ResponseEntity<WalletResponse> withdrawFunds(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody WithdrawRequest withdrawRequest) {

        User user = userService.getUserById(currentUser.getId());
        Wallet wallet = walletService.withdrawFunds(user, withdrawRequest.getAmount(),
                withdrawRequest.getPaymentMethodId());

        return new ResponseEntity<>(WalletResponse.fromEntity(wallet), HttpStatus.OK);
    }

    @GetMapping("/{walletId}/transactions")
    @Operation(summary = "Get transactions for a wallet")
    public ResponseEntity<Page<TransactionResponse>> getWalletTransactions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long walletId,
            Pageable pageable) {

        Wallet wallet = walletService.getWalletById(walletId);

        // Check if the wallet belongs to the current user
        if (!wallet.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Page<Transaction> transactions = transactionService.getWalletTransactions(wallet, pageable);

        Page<TransactionResponse> transactionResponses = transactions.map(TransactionResponse::fromEntity);

        return ResponseEntity.ok(transactionResponses);
    }
}
