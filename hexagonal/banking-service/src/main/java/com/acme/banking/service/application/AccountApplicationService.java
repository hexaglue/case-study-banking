package com.acme.banking.service.application;

import com.acme.banking.core.exception.AccountNotFoundException;
import com.acme.banking.core.model.Account;
import com.acme.banking.core.model.AccountId;
import com.acme.banking.core.model.AccountType;
import com.acme.banking.core.model.CustomerId;
import com.acme.banking.core.model.Money;
import com.acme.banking.core.model.Transaction;
import com.acme.banking.core.model.TransactionType;
import com.acme.banking.core.port.in.AccountUseCases;
import com.acme.banking.core.port.out.AccountRepository;
import com.acme.banking.core.port.out.CustomerRepository;
import com.acme.banking.core.port.out.TransactionRepository;

import java.util.List;

/**
 * Application service handling all account-related business logic.
 * Delegates business rules to the Account aggregate.
 *
 * @since 1.0.0
 */
public class AccountApplicationService implements AccountUseCases {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Creates a new AccountApplicationService with required dependencies.
     *
     * @param accountRepository     repository for account operations
     * @param customerRepository    repository for customer operations
     * @param transactionRepository repository for transaction operations
     */
    public AccountApplicationService(
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Opens a new account for a customer.
     *
     * @param customerId    the customer ID
     * @param type          the account type
     * @param accountNumber the account number
     * @return the created account
     * @throws RuntimeException if customer not found
     */
    @Override
    public Account openAccount(CustomerId customerId, AccountType type, String accountNumber) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId.value()));

        Account account = Account.open(accountNumber, type, customerId);
        return accountRepository.save(account);
    }

    /**
     * Closes an account by marking it as inactive.
     *
     * @param accountId the account ID
     * @throws AccountNotFoundException if account not found
     */
    @Override
    public void closeAccount(AccountId accountId) {
        Account account = getAccount(accountId);
        account.close();
        accountRepository.save(account);
    }

    /**
     * Gets the current balance of an account.
     *
     * @param accountId the account ID
     * @return the account balance
     * @throws AccountNotFoundException if account not found
     */
    @Override
    public Money getBalance(AccountId accountId) {
        Account account = getAccount(accountId);
        return account.getBalance();
    }

    /**
     * Gets an account by ID.
     *
     * @param accountId the account ID
     * @return the account
     * @throws AccountNotFoundException if account not found
     */
    @Override
    public Account getAccount(AccountId accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.value()));
    }

    /**
     * Gets an account by account number.
     *
     * @param accountNumber the account number
     * @return the account, or null if not found
     */
    @Override
    public Account getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).orElse(null);
    }

    /**
     * Gets all accounts for a customer.
     *
     * @param customerId the customer ID
     * @return list of accounts
     */
    @Override
    public List<Account> getAccountsByCustomer(CustomerId customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    /**
     * Gets all accounts.
     *
     * @return list of all accounts
     */
    @Override
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Deposits money into an account.
     * Delegates balance update to Account aggregate.
     *
     * @param accountId the account ID
     * @param amount    the amount to deposit
     * @return the updated account
     * @throws AccountNotFoundException if account not found
     */
    @Override
    public Account deposit(AccountId accountId, Money amount) {
        Account account = getAccount(accountId);
        account.deposit(amount);

        Transaction transaction = Transaction.create(
                accountId, amount, TransactionType.DEPOSIT, "Deposit", null);
        transactionRepository.save(transaction);

        return accountRepository.save(account);
    }

    /**
     * Withdraws money from an account.
     * Delegates balance check and update to Account aggregate.
     *
     * @param accountId the account ID
     * @param amount    the amount to withdraw
     * @return the updated account
     * @throws AccountNotFoundException                              if account not found
     * @throws com.acme.banking.core.exception.InsufficientFundsException if insufficient funds
     */
    @Override
    public Account withdraw(AccountId accountId, Money amount) {
        Account account = getAccount(accountId);
        account.withdraw(amount);

        Transaction transaction = Transaction.create(
                accountId, amount, TransactionType.WITHDRAWAL, "Withdrawal", null);
        transactionRepository.save(transaction);

        return accountRepository.save(account);
    }
}
