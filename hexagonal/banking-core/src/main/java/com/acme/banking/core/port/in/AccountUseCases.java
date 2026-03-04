package com.acme.banking.core.port.in;

import com.acme.banking.core.model.Account;
import com.acme.banking.core.model.AccountId;
import com.acme.banking.core.model.AccountType;
import com.acme.banking.core.model.CustomerId;
import com.acme.banking.core.model.Money;

import java.util.List;

/**
 * Driving port for account management use cases.
 *
 * @since 1.0.0
 */
public interface AccountUseCases {

    Account openAccount(CustomerId customerId, AccountType type, String accountNumber);

    void closeAccount(AccountId accountId);

    Money getBalance(AccountId accountId);

    Account getAccount(AccountId accountId);

    Account getAccountByNumber(String accountNumber);

    List<Account> getAccountsByCustomer(CustomerId customerId);

    List<Account> getAllAccounts();

    Account deposit(AccountId accountId, Money amount);

    Account withdraw(AccountId accountId, Money amount);
}
