package com.acme.banking.api.dto;

import com.acme.banking.core.model.Transfer;
import com.acme.banking.core.model.TransferStatus;
import java.math.BigDecimal;

public record TransferResponse(
    Long id,
    Long sourceAccountId,
    Long targetAccountId,
    BigDecimal amount,
    String currency,
    TransferStatus status,
    String reason
) {
    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
            transfer.getId(),
            transfer.getSourceAccount().getId(),
            transfer.getTargetAccount().getId(),
            transfer.getAmount(),
            transfer.getCurrency(),
            transfer.getStatus(),
            transfer.getReason()
        );
    }
}
