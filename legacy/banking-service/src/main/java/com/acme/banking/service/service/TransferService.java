package com.acme.banking.service.service;

import com.acme.banking.core.exception.AccountNotFoundException;
import com.acme.banking.core.exception.InsufficientFundsException;
import com.acme.banking.core.exception.TransferRejectedException;
import com.acme.banking.core.model.Account;
import com.acme.banking.core.model.Transaction;
import com.acme.banking.core.model.TransactionType;
import com.acme.banking.core.model.Transfer;
import com.acme.banking.core.model.TransferStatus;
import com.acme.banking.persistence.repository.AccountRepository;
import com.acme.banking.persistence.repository.TransactionRepository;
import com.acme.banking.persistence.repository.TransferRepository;
import com.acme.banking.service.event.TransferCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service orchestrating transfer operations between accounts.
 * <p>
 * ANTI-PATTERN: Complex orchestration logic mixed with business rules in the service layer.
 * Uses Spring ApplicationEvent instead of domain events, coupling business logic to framework.
 * Transfer business logic should be encapsulated in a Transfer aggregate root.
 * </p>
 *
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates a new TransferService with required dependencies.
     *
     * @param transferRepository     repository for transfer operations
     * @param accountRepository      repository for account operations
     * @param transactionRepository  repository for transaction operations
     * @param eventPublisher         Spring event publisher
     */
    public TransferService(
            TransferRepository transferRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            ApplicationEventPublisher eventPublisher) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Initiates a new transfer between accounts.
     * <p>
     * ANTI-PATTERN: Orchestration logic in service layer. Business rules about
     * what makes a valid transfer should be in the Transfer aggregate.
     * </p>
     *
     * @param sourceAccountId the source account ID
     * @param targetAccountId the target account ID
     * @param amount          the amount to transfer
     * @param reason          the transfer reason
     * @return the created transfer in PENDING status
     * @throws AccountNotFoundException   if either account not found
     * @throws InsufficientFundsException if source account has insufficient funds
     */
    @Transactional
    public Transfer initiateTransfer(Long sourceAccountId, Long targetAccountId, BigDecimal amount, String reason) {
        // Validate accounts exist
        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new AccountNotFoundException(sourceAccountId));
        Account targetAccount = accountRepository.findById(targetAccountId)
                .orElseThrow(() -> new AccountNotFoundException(targetAccountId));

        // Validate sufficient funds - business logic in service layer
        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(sourceAccount.getAccountNumber(), amount, sourceAccount.getBalance());
        }

        // Create transfer
        Transfer transfer = new Transfer();
        transfer.setSourceAccount(sourceAccount);
        transfer.setTargetAccount(targetAccount);
        transfer.setAmount(amount);
        transfer.setReason(reason);
        transfer.setStatus(TransferStatus.PENDING);

        return transferRepository.save(transfer);
    }

    /**
     * Executes a pending transfer.
     * <p>
     * ANTI-PATTERN: Complex orchestration spanning multiple aggregates (Transfer, Account, Transaction).
     * The service becomes a procedural script manipulating entities. Uses Spring ApplicationEvent
     * instead of domain events.
     * </p>
     *
     * @param transferId the transfer ID
     * @return the completed transfer
     * @throws RuntimeException          if transfer not found
     * @throws TransferRejectedException if transfer cannot be executed
     */
    @Transactional
    public Transfer executeTransfer(Long transferId) {
        Transfer transfer = getTransfer(transferId);

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new TransferRejectedException(transferId, "Transfer is not in PENDING status");
        }

        Account sourceAccount = transfer.getSourceAccount();
        Account targetAccount = transfer.getTargetAccount();
        BigDecimal amount = transfer.getAmount();

        // Debit source account - business logic in service layer
        BigDecimal newSourceBalance = sourceAccount.getBalance().subtract(amount);
        sourceAccount.setBalance(newSourceBalance);
        accountRepository.save(sourceAccount);

        // Credit target account - business logic in service layer
        BigDecimal newTargetBalance = targetAccount.getBalance().add(amount);
        targetAccount.setBalance(newTargetBalance);
        accountRepository.save(targetAccount);

        // Create transaction records
        Transaction transferOut = new Transaction();
        transferOut.setAccount(sourceAccount);
        transferOut.setAmount(amount.negate());
        transferOut.setType(TransactionType.TRANSFER_OUT);
        transferOut.setDescription("Transfer to " + targetAccount.getAccountNumber());
        transferOut.setReference(transfer.getId().toString());
        transactionRepository.save(transferOut);

        Transaction transferIn = new Transaction();
        transferIn.setAccount(targetAccount);
        transferIn.setAmount(amount);
        transferIn.setType(TransactionType.TRANSFER_IN);
        transferIn.setDescription("Transfer from " + sourceAccount.getAccountNumber());
        transferIn.setReference(transfer.getId().toString());
        transactionRepository.save(transferIn);

        // Update transfer status
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer = transferRepository.save(transfer);

        // Publish Spring event - ANTI-PATTERN: framework coupling
        eventPublisher.publishEvent(new TransferCompletedEvent(this, transfer));

        return transfer;
    }

    /**
     * Cancels a pending transfer.
     *
     * @param transferId the transfer ID
     * @return the cancelled transfer
     * @throws RuntimeException if transfer not found
     */
    @Transactional
    public Transfer cancelTransfer(Long transferId) {
        Transfer transfer = getTransfer(transferId);
        transfer.setStatus(TransferStatus.CANCELLED);
        return transferRepository.save(transfer);
    }

    /**
     * Gets a transfer by ID.
     *
     * @param id the transfer ID
     * @return the transfer
     * @throws RuntimeException if transfer not found
     */
    public Transfer getTransfer(Long id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transfer not found: " + id));
    }

    /**
     * Gets all transfers for an account (as source).
     *
     * @param accountId the account ID
     * @return list of transfers
     */
    public List<Transfer> getTransfersByAccount(Long accountId) {
        return transferRepository.findBySourceAccountId(accountId);
    }
}
