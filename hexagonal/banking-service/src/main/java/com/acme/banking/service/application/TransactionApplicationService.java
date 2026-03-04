package com.acme.banking.service.application;

import com.acme.banking.core.exception.AccountNotFoundException;
import com.acme.banking.core.model.AccountId;
import com.acme.banking.core.model.Money;
import com.acme.banking.core.model.Transaction;
import com.acme.banking.core.model.TransactionType;
import com.acme.banking.core.port.in.TransactionUseCases;
import com.acme.banking.core.port.out.AccountRepository;
import com.acme.banking.core.port.out.TransactionRepository;

import java.util.List;

/**
 * Application service handling transaction recording and history operations.
 *
 * @since 1.0.0
 */
public class TransactionApplicationService implements TransactionUseCases {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    /**
     * Creates a new TransactionApplicationService with required dependencies.
     *
     * @param transactionRepository repository for transaction operations
     * @param accountRepository     repository for account operations
     */
    public TransactionApplicationService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Records a transaction for an account.
     *
     * @param accountId   the account ID
     * @param amount      the transaction amount
     * @param type        the transaction type
     * @param description the transaction description
     * @param reference   optional reference
     * @return the created transaction
     * @throws AccountNotFoundException if account not found
     */
    @Override
    public Transaction recordTransaction(
            AccountId accountId,
            Money amount,
            TransactionType type,
            String description,
            String reference) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.value()));

        Transaction transaction = Transaction.create(accountId, amount, type, description, reference);
        return transactionRepository.save(transaction);
    }

    /**
     * Gets transaction history for an account, ordered by date descending.
     *
     * @param accountId the account ID
     * @return list of transactions
     */
    @Override
    public List<Transaction> getHistory(AccountId accountId) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    /**
     * Gets transactions for an account filtered by type.
     *
     * @param accountId the account ID
     * @param type      the transaction type
     * @return list of transactions
     */
    @Override
    public List<Transaction> getStatement(AccountId accountId, TransactionType type) {
        return transactionRepository.findByAccountIdAndType(accountId, type);
    }
}
