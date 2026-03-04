package com.acme.banking.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TransferRequest(
    @NotNull Long sourceAccountId,
    @NotNull Long targetAccountId,
    @NotNull @Positive BigDecimal amount,
    String reason
) {}
