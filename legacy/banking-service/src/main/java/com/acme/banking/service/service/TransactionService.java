package com.acme.banking.service.service;

import com.acme.banking.core.exception.AccountNotFoundException;
import com.acme.banking.core.model.Account;
import com.acme.banking.core.model.Transaction;
import com.acme.banking.core.model.TransactionType;
import com.acme.banking.persistence.repository.AccountRepository;
import com.acme.banking.persistence.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service handling transaction recording and history operations.
 * <p>
 * ANTI-PATTERN: Transaction recording is split between multiple services (AccountService,
 * TransferService, and this service), leading to duplication and inconsistency. Transaction
 * creation should be encapsulated in the Account aggregate.
 * </p>
 *
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    /**
     * Creates a new TransactionService with required dependencies.
     *
     * @param transactionRepository repository for transaction operations
     * @param accountRepository     repository for account operations
     */
    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Records a transaction for an account.
     * <p>
     * ANTI-PATTERN: Transaction creation logic duplicated across multiple services.
     * Should be a factory method on Account aggregate.
     * </p>
     *
     * @param accountId   the account ID
     * @param amount      the transaction amount
     * @param type        the transaction type
     * @param description the transaction description
     * @param reference   optional reference
     * @return the created transaction
     * @throws AccountNotFoundException if account not found
     */
    @Transactional
    public Transaction recordTransaction(
            Long accountId,
            BigDecimal amount,
            TransactionType type,
            String description,
            String reference) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setReference(reference);

        return transactionRepository.save(transaction);
    }

    /**
     * Gets transaction history for an account, ordered by date descending.
     *
     * @param accountId the account ID
     * @return list of transactions
     */
    public List<Transaction> getHistory(Long accountId) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    /**
     * Gets transactions for an account filtered by type.
     *
     * @param accountId the account ID
     * @param type      the transaction type
     * @return list of transactions
     */
    public List<Transaction> getStatement(Long accountId, TransactionType type) {
        return transactionRepository.findByAccountIdAndType(accountId, type);
    }
}
