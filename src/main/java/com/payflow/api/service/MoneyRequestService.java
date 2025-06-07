package com.payflow.api.service;

import com.payflow.api.exception.BadRequestException;
import com.payflow.api.exception.ResourceNotFoundException;
import com.payflow.api.model.dto.request.MoneyRequestActionDTO;
import com.payflow.api.model.dto.request.MoneyRequestDTO;
import com.payflow.api.model.entity.MoneyRequest;
import com.payflow.api.model.entity.Transaction;
import com.payflow.api.model.entity.User;
import com.payflow.api.model.entity.Wallet;
import com.payflow.api.repository.MoneyRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MoneyRequestService {

    private final MoneyRequestRepository moneyRequestRepository;
    private final UserService userService;
    private final WalletService walletService;
    private final TransactionService transactionService;

    @Transactional
    public MoneyRequest createMoneyRequest(User requester, MoneyRequestDTO requestDTO) {
        User requestee = userService.getUserByEmail(requestDTO.getRequesteeEmail());

        // Ensure requester is not requesting money from themselves
        if (requester.getId().equals(requestee.getId())) {
            throw new BadRequestException("You cannot request money from yourself");
        }
        Wallet wallet = walletService.getWalletByNumber(requestDTO.getWalletNumber());

        // Ensure the wallet belongs to the requester
        if (!wallet.getUser().getId().equals(requester.getId())) {
            throw new BadRequestException("You can only request money to your own wallet");
        }

        // Create the money request
        MoneyRequest moneyRequest = new MoneyRequest();
        moneyRequest.setRequester(requester);
        moneyRequest.setRequestee(requestee);
        moneyRequest.setRequestWallet(wallet);
        moneyRequest.setAmount(requestDTO.getAmount());
        moneyRequest.setDescription(requestDTO.getDescription());
        moneyRequest.setStatus(MoneyRequest.RequestStatus.PENDING);
        moneyRequest.setExpiresAt(LocalDateTime.now().plusDays(7));

        return moneyRequestRepository.save(moneyRequest);
    }

    public MoneyRequest getMoneyRequestById(Long id) {
        return moneyRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MoneyRequest", "id", id));
    }

    public MoneyRequest getMoneyRequestByNumber(String requestNumber) {
        return moneyRequestRepository.findByRequestNumber(requestNumber)
                .orElseThrow(() -> new ResourceNotFoundException("MoneyRequest", "requestNumber", requestNumber));
    }

    public Page<MoneyRequest> getSentMoneyRequests(User user, Pageable pageable) {
        return moneyRequestRepository.findByRequesterOrderByCreatedAtDesc(user, pageable);
    }

    public Page<MoneyRequest> getReceivedMoneyRequests(User user, Pageable pageable) {
        return moneyRequestRepository.findByRequesteeOrderByCreatedAtDesc(user, pageable);
    }

    @Transactional
    public Transaction processMoneyRequestAction(User user, MoneyRequestActionDTO actionDTO) {
        MoneyRequest moneyRequest = getMoneyRequestByNumber(actionDTO.getRequestNumber());

        // Ensure the user is the requestee
        if (!moneyRequest.getRequestee().getId().equals(user.getId())) {
            throw new BadRequestException("You can only respond to money requests sent to you");
        }

        // Ensure the money request is pending
        if (moneyRequest.getStatus() != MoneyRequest.RequestStatus.PENDING) {
            throw new BadRequestException(
                    "This money request has already been " + moneyRequest.getStatus().name().toLowerCase());
        }

        // Process based on action
        String action = actionDTO.getAction().toUpperCase();
        switch (action) {
            case "APPROVE":
                return approveMoneyRequest(moneyRequest, user, actionDTO.getPaymentWalletNumber());
            case "DECLINE":
                declineMoneyRequest(moneyRequest);
                return null;
            default:
                throw new BadRequestException("Invalid action. Must be either 'APPROVE' or 'DECLINE'");
        }
    }

    private Transaction approveMoneyRequest(MoneyRequest moneyRequest, User user, String paymentWalletNumber) {
        Wallet sourceWallet = walletService.getWalletByNumber(paymentWalletNumber);

        // Ensure the wallet belongs to the user
        if (!sourceWallet.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only pay with your own wallet");
        }

        // Create a transaction for the payment
        Transaction transaction = transactionService.createMoneyRequestTransaction(moneyRequest, sourceWallet);

        // Update the money request status
        moneyRequest.setStatus(MoneyRequest.RequestStatus.APPROVED);
        moneyRequestRepository.save(moneyRequest);

        return transaction;
    }

    private void declineMoneyRequest(MoneyRequest moneyRequest) {
        moneyRequest.setStatus(MoneyRequest.RequestStatus.DECLINED);
        moneyRequestRepository.save(moneyRequest);
    }

    @Transactional
    public void cancelMoneyRequest(User user, String requestNumber) {
        MoneyRequest moneyRequest = getMoneyRequestByNumber(requestNumber);

        // Ensure the user is the requester
        if (!moneyRequest.getRequester().getId().equals(user.getId())) {
            throw new BadRequestException("You can only cancel your own money requests");
        }

        // Ensure the money request is pending
        if (moneyRequest.getStatus() != MoneyRequest.RequestStatus.PENDING) {
            throw new BadRequestException(
                    "This money request has already been " + moneyRequest.getStatus().name().toLowerCase());
        }

        // Update the money request status
        moneyRequest.setStatus(MoneyRequest.RequestStatus.CANCELLED);
        moneyRequestRepository.save(moneyRequest);
    }

    @Scheduled(cron = "0 0 0 * * ?") // Run every day at midnight
    public void expireMoneyRequests() {
        LocalDateTime now = LocalDateTime.now();
        List<MoneyRequest> expiredRequests = moneyRequestRepository.findExpiredRequests(now);

        for (MoneyRequest request : expiredRequests) {
            request.setStatus(MoneyRequest.RequestStatus.EXPIRED);
            moneyRequestRepository.save(request);
        }
    }

    public List<MoneyRequest> getPendingRequestsForUser(User user) {
        return moneyRequestRepository.findPendingRequestsForUser(user);
    }
}
