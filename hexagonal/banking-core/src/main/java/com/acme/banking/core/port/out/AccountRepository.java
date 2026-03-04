package com.acme.banking.core.port.out;

import com.acme.banking.core.model.Account;
import com.acme.banking.core.model.AccountId;
import com.acme.banking.core.model.CustomerId;

import java.util.List;
import java.util.Optional;

/**
 * Driven port for account persistence operations.
 *
 * @since 1.0.0
 */
public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(AccountId id);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(CustomerId customerId);

    List<Account> findByActiveTrue();

    List<Account> findAll();
}
