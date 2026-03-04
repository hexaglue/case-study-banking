package com.acme.banking.core.port.out;

import com.acme.banking.core.model.AccountId;
import com.acme.banking.core.model.Transaction;
import com.acme.banking.core.model.TransactionType;

import java.util.List;

/**
 * Driven port for transaction persistence operations.
 *
 * @since 1.0.0
 */
public interface TransactionRepository {

    Transaction save(Transaction transaction);

    List<Transaction> findByAccountId(AccountId accountId);

    List<Transaction> findByAccountIdAndType(AccountId accountId, TransactionType type);

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(AccountId accountId);
}
