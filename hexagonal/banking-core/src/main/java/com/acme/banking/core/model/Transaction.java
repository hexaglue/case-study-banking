package com.acme.banking.core.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Transaction entity representing a financial transaction.
 * Immutable after creation.
 *
 * @since 2.0.0
 */
public class Transaction {

    private TransactionId id;
    private Money amount;
    private TransactionType type;
    private String description;
    private String reference;
    private AccountId accountId;
    private LocalDateTime createdAt;

    private Transaction() {
    }

    /**
     * Creates a new transaction.
     *
     * @param accountId   the account identifier
     * @param amount      the transaction amount
     * @param type        the transaction type
     * @param description the description
     * @param reference   optional reference
     * @return a new transaction
     */
    public static Transaction create(
            AccountId accountId, Money amount, TransactionType type, String description, String reference) {
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Transaction tx = new Transaction();
        tx.accountId = accountId;
        tx.amount = amount;
        tx.type = type;
        tx.description = description;
        tx.reference = reference;
        tx.createdAt = LocalDateTime.now();
        return tx;
    }

    /**
     * Reconstitutes a transaction from persisted state.
     * <p>
     * This factory method restores a transaction without triggering any business logic
     * or validation. It is used by infrastructure mappers to rebuild domain objects
     * from the database.
     * </p>
     *
     * @param id          the transaction identifier
     * @param accountId   the account identifier
     * @param amount      the transaction amount
     * @param type        the transaction type
     * @param description the description
     * @param reference   the reference
     * @param createdAt   the creation timestamp
     * @return a reconstituted transaction instance
     */
    public static Transaction reconstitute(
            TransactionId id,
            AccountId accountId,
            Money amount,
            TransactionType type,
            String description,
            String reference,
            LocalDateTime createdAt) {
        Transaction tx = new Transaction();
        tx.id = id;
        tx.accountId = accountId;
        tx.amount = amount;
        tx.type = type;
        tx.description = description;
        tx.reference = reference;
        tx.createdAt = createdAt;
        return tx;
    }

    public TransactionId getId() {
        return id;
    }

    public void setId(TransactionId id) {
        this.id = id;
    }

    public Money getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getReference() {
        return reference;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
