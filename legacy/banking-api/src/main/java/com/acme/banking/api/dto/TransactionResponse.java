package com.acme.banking.api.dto;

import com.acme.banking.core.model.Transaction;
import com.acme.banking.core.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long id,
    BigDecimal amount,
    String currency,
    TransactionType type,
    String description,
    String reference,
    LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getType(),
            transaction.getDescription(),
            transaction.getReference(),
            transaction.getCreatedAt()
        );
    }
}
