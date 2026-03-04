package com.acme.banking.core.port.in;

import com.acme.banking.core.model.AccountId;
import com.acme.banking.core.model.Money;
import com.acme.banking.core.model.Transaction;
import com.acme.banking.core.model.TransactionType;

import java.util.List;

/**
 * Driving port for transaction recording and history use cases.
 *
 * @since 1.0.0
 */
public interface TransactionUseCases {

    Transaction recordTransaction(AccountId accountId, Money amount, TransactionType type, String description,
            String reference);

    List<Transaction> getHistory(AccountId accountId);

    List<Transaction> getStatement(AccountId accountId, TransactionType type);
}
