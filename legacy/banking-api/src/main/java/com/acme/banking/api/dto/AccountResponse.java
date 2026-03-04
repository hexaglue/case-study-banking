package com.acme.banking.api.dto;

import com.acme.banking.core.model.Account;
import com.acme.banking.core.model.AccountType;
import java.math.BigDecimal;

public record AccountResponse(
    Long id,
    String accountNumber,
    BigDecimal balance,
    AccountType type,
    boolean active,
    Long customerId
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getAccountNumber(),
            account.getBalance(),
            account.getType(),
            account.isActive(),
            account.getCustomer() != null ? account.getCustomer().getId() : null
        );
    }
}
