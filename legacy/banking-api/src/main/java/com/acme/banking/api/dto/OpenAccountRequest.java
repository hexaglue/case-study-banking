package com.acme.banking.api.dto;

import com.acme.banking.core.model.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OpenAccountRequest(
    @NotNull Long customerId,
    @NotNull AccountType type,
    @NotBlank String accountNumber
) {}
