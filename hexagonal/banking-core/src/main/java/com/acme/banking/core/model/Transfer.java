package com.acme.banking.core.model;

import java.util.Objects;

/**
 * Transfer aggregate root representing a money transfer between accounts.
 * Manages state transitions: PENDING → COMPLETED or CANCELLED.
 *
 * @since 2.0.0
 */
public class Transfer {

    private TransferId id;
    private AccountId sourceAccountId;
    private AccountId targetAccountId;
    private Money amount;
    private TransferStatus status;
    private String reason;

    private Transfer() {
    }

    /**
     * Initiates a new transfer.
     *
     * @param sourceAccountId the source account identifier
     * @param targetAccountId the target account identifier
     * @param amount          the transfer amount
     * @param reason          the transfer reason
     * @return a new transfer in PENDING status
     */
    public static Transfer initiate(AccountId sourceAccountId, AccountId targetAccountId, Money amount, String reason) {
        Objects.requireNonNull(sourceAccountId, "sourceAccountId must not be null");
        Objects.requireNonNull(targetAccountId, "targetAccountId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Transfer transfer = new Transfer();
        transfer.sourceAccountId = sourceAccountId;
        transfer.targetAccountId = targetAccountId;
        transfer.amount = amount;
        transfer.reason = reason;
        transfer.status = TransferStatus.PENDING;
        return transfer;
    }

    /**
     * Reconstitutes a transfer from persisted state.
     * <p>
     * This factory method restores a transfer without triggering any business logic
     * or state validation. It is used by infrastructure mappers to rebuild domain objects
     * from the database.
     * </p>
     *
     * @param id              the transfer identifier
     * @param sourceAccountId the source account identifier
     * @param targetAccountId the target account identifier
     * @param amount          the transfer amount
     * @param reason          the transfer reason
     * @param status          the current transfer status
     * @return a reconstituted transfer instance
     */
    public static Transfer reconstitute(
            TransferId id,
            AccountId sourceAccountId,
            AccountId targetAccountId,
            Money amount,
            String reason,
            TransferStatus status) {
        Transfer transfer = new Transfer();
        transfer.id = id;
        transfer.sourceAccountId = sourceAccountId;
        transfer.targetAccountId = targetAccountId;
        transfer.amount = amount;
        transfer.reason = reason;
        transfer.status = status;
        return transfer;
    }

    /**
     * Marks this transfer as completed.
     *
     * @throws IllegalStateException if the transfer is not in PENDING status
     */
    public void execute() {
        if (this.status != TransferStatus.PENDING) {
            throw new IllegalStateException("Transfer must be in PENDING status to execute, current: " + this.status);
        }
        this.status = TransferStatus.COMPLETED;
    }

    /**
     * Cancels this transfer.
     *
     * @throws IllegalStateException if the transfer is not in PENDING status
     */
    public void cancel() {
        if (this.status != TransferStatus.PENDING) {
            throw new IllegalStateException("Transfer must be in PENDING status to cancel, current: " + this.status);
        }
        this.status = TransferStatus.CANCELLED;
    }

    public TransferId getId() {
        return id;
    }

    public void setId(TransferId id) {
        this.id = id;
    }

    public AccountId getSourceAccountId() {
        return sourceAccountId;
    }

    public AccountId getTargetAccountId() {
        return targetAccountId;
    }

    public Money getAmount() {
        return amount;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}
