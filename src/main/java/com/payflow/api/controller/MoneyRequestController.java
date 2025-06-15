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
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Controller for handling money request operations. */
@RestController
@RequestMapping("money-requests")
@RequiredArgsConstructor
@Tag(name = "Money Requests", description = "Money Request management API")
public class MoneyRequestController {

  private final MoneyRequestService moneyRequestService;
  private final UserService userService;

  /**
   * Creates a new money request.
   *
   * @param currentUser the authenticated user making the request
   * @param requestDto the money request details
   * @return the created money request
   */
  @PostMapping
  @Operation(summary = "Request money from another user")
  public ResponseEntity<MoneyRequestResponse> requestMoney(
      @AuthenticationPrincipal final UserPrincipal currentUser,
      @Valid @RequestBody final MoneyRequestDTO requestDto) {

    final User requester = userService.getUserById(currentUser.getId());
    final MoneyRequest moneyRequest = moneyRequestService.createMoneyRequest(requester, requestDto);
    return new ResponseEntity<>(MoneyRequestResponse.fromEntity(moneyRequest), HttpStatus.CREATED);
  }

  /**
   * Retrieves money requests sent by the current user.
   *
   * @param currentUser the authenticated user
   * @param pageable pagination parameters
   * @return page of sent money requests
   */
  @GetMapping("/sent")
  @Operation(summary = "Get money requests sent by current user")
  public ResponseEntity<Page<MoneyRequestResponse>> getSentRequests(
      @AuthenticationPrincipal final UserPrincipal currentUser, final Pageable pageable) {

    final User user = userService.getUserById(currentUser.getId());
    final Page<MoneyRequest> requests = moneyRequestService.getSentMoneyRequests(user, pageable);

    final Page<MoneyRequestResponse> responses = requests.map(MoneyRequestResponse::fromEntity);

    return ResponseEntity.ok(responses);
  }

  /**
   * Retrieves money requests received by the current user.
   *
   * @param currentUser the authenticated user
   * @param pageable pagination parameters
   * @return page of received money requests
   */
  @GetMapping("/received")
  @Operation(summary = "Get money requests received by current user")
  public ResponseEntity<Page<MoneyRequestResponse>> getReceivedRequests(
      @AuthenticationPrincipal final UserPrincipal currentUser, final Pageable pageable) {

    final User user = userService.getUserById(currentUser.getId());
    final Page<MoneyRequest> requests =
        moneyRequestService.getReceivedMoneyRequests(user, pageable);

    final Page<MoneyRequestResponse> responses = requests.map(MoneyRequestResponse::fromEntity);

    return ResponseEntity.ok(responses);
  }

  /**
   * Processes a money request (approve or decline).
   *
   * @param currentUser the authenticated user
   * @param actionDto the action to perform
   * @return transaction response if approved, empty response if declined
   */
  @PostMapping("/process")
  @Operation(summary = "Approve or decline a money request")
  public ResponseEntity<?> processMoneyRequest(
      @AuthenticationPrincipal final UserPrincipal currentUser,
      @Valid @RequestBody final MoneyRequestActionDTO actionDto) {

    final User user = userService.getUserById(currentUser.getId());

    if ("APPROVE".equalsIgnoreCase(actionDto.getAction())) {
      final Transaction transaction =
          moneyRequestService.processMoneyRequestAction(user, actionDto);
      return ResponseEntity.ok(TransactionResponse.fromEntity(transaction));
    } else {
      moneyRequestService.processMoneyRequestAction(user, actionDto);
      return ResponseEntity.ok().build();
    }
  }

  /**
   * Cancels a money request.
   *
   * @param currentUser the authenticated user
   * @param requestNumber the request number to cancel
   * @return empty response
   */
  @PostMapping("/{requestNumber}/cancel")
  @Operation(summary = "Cancel a money request")
  public ResponseEntity<?> cancelMoneyRequest(
      @AuthenticationPrincipal final UserPrincipal currentUser,
      @PathVariable final String requestNumber) {

    final User user = userService.getUserById(currentUser.getId());
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
