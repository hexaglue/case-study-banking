package com.acme.banking.api.controller;

import com.acme.banking.api.dto.TransactionResponse;
import com.acme.banking.core.model.Transaction;
import com.acme.banking.core.model.TransactionType;
import com.acme.banking.service.service.TransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/account/{accountId}")
    public List<TransactionResponse> getAccountHistory(@PathVariable Long accountId) {
        List<Transaction> transactions = transactionService.getHistory(accountId);
        return transactions.stream()
            .map(TransactionResponse::from)
            .toList();
    }

    @GetMapping("/account/{accountId}/statement")
    public List<TransactionResponse> getAccountStatement(
        @PathVariable Long accountId,
        @RequestParam(required = false) TransactionType type
    ) {
        List<Transaction> transactions = transactionService.getStatement(accountId, type);
        return transactions.stream()
            .map(TransactionResponse::from)
            .toList();
    }
}
