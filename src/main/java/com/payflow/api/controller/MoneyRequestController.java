package com.payflow.api.controller;

import com.payflow.api.model.dto.request.MoneyRequestActionDTO;
import com.payflow.api.model.dto.request.MoneyRequestDTO;
import com.payflow.api.model.dto.response.MoneyRequestResponse;
import com.payflow.api.model.dto.response.TransactionResponse;
import com.payflow.api.model.entity.MoneyRequest;
import com.payflow.api.model.entity.Transaction;
import com.payflow.api.model.entity.User;
import com.payflow.api.security.UserPrincipal;
import com.payflow.api.service.MoneyRequestService;
import com.payflow.api.service.UserService;
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

@RestController
@RequestMapping("money-requests")
@RequiredArgsConstructor
@Tag(name = "Money Requests", description = "Money Request management API")
public class MoneyRequestController {

    private final MoneyRequestService moneyRequestService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "Request money from another user")
    public ResponseEntity<MoneyRequestResponse> requestMoney(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody MoneyRequestDTO requestDTO) {

        User requester = userService.getUserById(currentUser.getId());
        MoneyRequest moneyRequest = moneyRequestService.createMoneyRequest(requester, requestDTO);

        return new ResponseEntity<>(MoneyRequestResponse.fromEntity(moneyRequest), HttpStatus.CREATED);
    }

    @GetMapping("/sent")
    @Operation(summary = "Get money requests sent by current user")
    public ResponseEntity<Page<MoneyRequestResponse>> getSentRequests(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {

        User user = userService.getUserById(currentUser.getId());
        Page<MoneyRequest> requests = moneyRequestService.getSentMoneyRequests(user, pageable);

        Page<MoneyRequestResponse> responses = requests.map(MoneyRequestResponse::fromEntity);

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/received")
    @Operation(summary = "Get money requests received by current user")
    public ResponseEntity<Page<MoneyRequestResponse>> getReceivedRequests(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {

        User user = userService.getUserById(currentUser.getId());
        Page<MoneyRequest> requests = moneyRequestService.getReceivedMoneyRequests(user, pageable);

        Page<MoneyRequestResponse> responses = requests.map(MoneyRequestResponse::fromEntity);

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/process")
    @Operation(summary = "Approve or decline a money request")
    public ResponseEntity<?> processMoneyRequest(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody MoneyRequestActionDTO actionDTO) {

        User user = userService.getUserById(currentUser.getId());

        if ("APPROVE".equalsIgnoreCase(actionDTO.getAction())) {
            Transaction transaction = moneyRequestService.processMoneyRequestAction(user, actionDTO);
            return ResponseEntity.ok(TransactionResponse.fromEntity(transaction));
        } else {
            moneyRequestService.processMoneyRequestAction(user, actionDTO);
            return ResponseEntity.ok().build();
        }
    }

    @PostMapping("/{requestNumber}/cancel")
    @Operation(summary = "Cancel a money request")
    public ResponseEntity<?> cancelMoneyRequest(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable String requestNumber) {

        User user = userService.getUserById(currentUser.getId());
        moneyRequestService.cancelMoneyRequest(user, requestNumber);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending money requests for current user")
    public ResponseEntity<?> getPendingRequests(@AuthenticationPrincipal UserPrincipal currentUser) {
        User user = userService.getUserById(currentUser.getId());

        return ResponseEntity.ok(
                moneyRequestService.getPendingRequestsForUser(user).stream()
                        .map(MoneyRequestResponse::fromEntity)
                        .collect(java.util.stream.Collectors.toList()));
    }
}
