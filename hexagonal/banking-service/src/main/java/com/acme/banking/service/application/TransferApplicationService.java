package com.acme.banking.service.application;

import com.acme.banking.core.exception.AccountNotFoundException;
import com.acme.banking.core.exception.InsufficientFundsException;
import com.acme.banking.core.exception.TransferRejectedException;
import com.acme.banking.core.model.Account;
import com.acme.banking.core.model.AccountId;
import com.acme.banking.core.model.Money;
import com.acme.banking.core.model.Transaction;
import com.acme.banking.core.model.TransactionType;
import com.acme.banking.core.model.Transfer;
import com.acme.banking.core.model.TransferId;
import com.acme.banking.core.port.in.TransferUseCases;
import com.acme.banking.core.port.out.AccountRepository;
import com.acme.banking.core.port.out.FraudDetection;
import com.acme.banking.core.port.out.NotificationSender;
import com.acme.banking.core.port.out.TransactionRepository;
import com.acme.banking.core.port.out.TransferRepository;

import java.util.List;

/**
 * Application service orchestrating transfer operations between accounts.
 *
 * @since 1.0.0
 */
public class TransferApplicationService implements TransferUseCases {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FraudDetection fraudDetection;
    private final NotificationSender notificationSender;

    /**
     * Creates a new TransferApplicationService with required dependencies.
     *
     * @param transferRepository     repository for transfer operations
     * @param accountRepository      repository for account operations
     * @param transactionRepository  repository for transaction operations
     * @param fraudDetection         fraud detection gateway
     * @param notificationSender     notification sender
     */
    public TransferApplicationService(
            TransferRepository transferRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            FraudDetection fraudDetection,
            NotificationSender notificationSender) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.fraudDetection = fraudDetection;
        this.notificationSender = notificationSender;
    }

    /**
     * Initiates a new transfer between accounts.
     *
     * @param sourceAccountId the source account ID
     * @param targetAccountId the target account ID
     * @param amount          the amount to transfer
     * @param reason          the transfer reason
     * @return the created transfer in PENDING status
     * @throws AccountNotFoundException   if either account not found
     * @throws InsufficientFundsException if source account has insufficient funds
     */
    @Override
    public Transfer initiateTransfer(AccountId sourceAccountId, AccountId targetAccountId, Money amount, String reason) {
        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new AccountNotFoundException(sourceAccountId.value()));
        accountRepository.findById(targetAccountId)
                .orElseThrow(() -> new AccountNotFoundException(targetAccountId.value()));

        if (!sourceAccount.getBalance().isGreaterThanOrEqual(amount)) {
            throw new InsufficientFundsException(
                    sourceAccount.getAccountNumber(), amount.amount(), sourceAccount.getBalance().amount());
        }

        Transfer transfer = Transfer.initiate(sourceAccountId, targetAccountId, amount, reason);
        return transferRepository.save(transfer);
    }

    /**
     * Executes a pending transfer.
     *
     * @param transferId the transfer ID
     * @return the completed transfer
     * @throws RuntimeException          if transfer not found
     * @throws TransferRejectedException if transfer cannot be executed
     */
    @Override
    public Transfer executeTransfer(TransferId transferId) {
        Transfer transfer = getTransfer(transferId);

        if (!fraudDetection.checkTransfer(transfer)) {
            throw new TransferRejectedException(transfer.getId().value(), "Fraud detected");
        }

        AccountId sourceId = transfer.getSourceAccountId();
        AccountId targetId = transfer.getTargetAccountId();

        Account sourceAccount = accountRepository.findById(sourceId)
                .orElseThrow(() -> new AccountNotFoundException(sourceId.value()));
        Account targetAccount = accountRepository.findById(targetId)
                .orElseThrow(() -> new AccountNotFoundException(targetId.value()));

        Money amount = transfer.getAmount();

        sourceAccount.withdraw(amount);
        accountRepository.save(sourceAccount);

        targetAccount.deposit(amount);
        accountRepository.save(targetAccount);

        Transaction transferOut = Transaction.create(
                transfer.getSourceAccountId(),
                amount.negate(),
                TransactionType.TRANSFER_OUT,
                "Transfer to " + targetAccount.getAccountNumber(),
                transfer.getId().value().toString());
        transactionRepository.save(transferOut);

        Transaction transferIn = Transaction.create(
                transfer.getTargetAccountId(),
                amount,
                TransactionType.TRANSFER_IN,
                "Transfer from " + sourceAccount.getAccountNumber(),
                transfer.getId().value().toString());
        transactionRepository.save(transferIn);

        transfer.execute();
        Transfer saved = transferRepository.save(transfer);

        notificationSender.sendTransferNotification(saved);

        return saved;
    }

    /**
     * Cancels a pending transfer.
     *
     * @param transferId the transfer ID
     * @return the cancelled transfer
     * @throws RuntimeException if transfer not found
     */
    @Override
    public Transfer cancelTransfer(TransferId transferId) {
        Transfer transfer = getTransfer(transferId);
        transfer.cancel();
        return transferRepository.save(transfer);
    }

    /**
     * Gets a transfer by ID.
     *
     * @param id the transfer ID
     * @return the transfer
     * @throws RuntimeException if transfer not found
     */
    @Override
    public Transfer getTransfer(TransferId id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transfer not found: " + id.value()));
    }

    /**
     * Gets all transfers for an account (as source).
     *
     * @param accountId the account ID
     * @return list of transfers
     */
    @Override
    public List<Transfer> getTransfersByAccount(AccountId accountId) {
        return transferRepository.findBySourceAccountId(accountId);
    }
}
