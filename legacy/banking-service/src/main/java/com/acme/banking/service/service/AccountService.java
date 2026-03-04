package com.acme.banking.service.service;

import com.acme.banking.core.exception.AccountNotFoundException;
import com.acme.banking.core.exception.InsufficientFundsException;
import com.acme.banking.core.model.Account;
import com.acme.banking.core.model.AccountType;
import com.acme.banking.core.model.Customer;
import com.acme.banking.core.model.Transaction;
import com.acme.banking.core.model.TransactionType;
import com.acme.banking.persistence.repository.AccountRepository;
import com.acme.banking.persistence.repository.CustomerRepository;
import com.acme.banking.persistence.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service handling all account-related business logic.
 * <p>
 * ANTI-PATTERN: Anemic domain model - all business logic is in the service layer
 * instead of being encapsulated in the Account entity. The Account entity becomes
 * a mere data holder with no behavior.
 * </p>
 *
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Creates a new AccountService with required dependencies.
     *
     * @param accountRepository     repository for account operations
     * @param customerRepository    repository for customer operations
     * @param transactionRepository repository for transaction operations
     */
    public AccountService(
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
    @Transactional
    public Account openAccount(Long customerId, AccountType type, String accountNumber) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setType(type);
        account.setBalance(BigDecimal.ZERO);
        account.setActive(true);
        account.setCustomer(customer);

        return accountRepository.save(account);
    }

    /**
     * Closes an account by marking it as inactive.
     *
     * @param accountId the account ID
     * @throws AccountNotFoundException if account not found
     */
    @Transactional
    public void closeAccount(Long accountId) {
        Account account = getAccount(accountId);
        account.setActive(false);
        accountRepository.save(account);
    }

    /**
     * Gets the current balance of an account.
     *
     * @param accountId the account ID
     * @return the account balance
     * @throws AccountNotFoundException if account not found
     */
    public BigDecimal getBalance(Long accountId) {
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
    public Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    /**
     * Gets an account by account number.
     *
     * @param accountNumber the account number
     * @return the account, or null if not found
     */
    public Account getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).orElse(null);
    }

    /**
     * Gets all accounts for a customer.
     *
     * @param customerId the customer ID
     * @return list of accounts
     */
    public List<Account> getAccountsByCustomer(Long customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    /**
     * Gets all accounts.
     *
     * @return list of all accounts
     */
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Deposits money into an account.
     * <p>
     * ANTI-PATTERN: Business logic (balance update) is in the service,
     * not in the Account entity where it belongs.
     * </p>
     *
     * @param accountId the account ID
     * @param amount    the amount to deposit
     * @return the updated account
     * @throws AccountNotFoundException if account not found
     */
    @Transactional
    public Account deposit(Long accountId, BigDecimal amount) {
        Account account = getAccount(accountId);

        // Business logic in service layer - should be in Account entity
        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setDescription("Deposit");
        transactionRepository.save(transaction);

        return accountRepository.save(account);
    }

    /**
     * Withdraws money from an account.
     * <p>
     * ANTI-PATTERN: Business logic (balance check and update) is in the service,
     * not in the Account entity where it belongs.
     * </p>
     *
     * @param accountId the account ID
     * @param amount    the amount to withdraw
     * @return the updated account
     * @throws AccountNotFoundException   if account not found
     * @throws InsufficientFundsException if insufficient funds
     */
    @Transactional
    public Account withdraw(Long accountId, BigDecimal amount) {
        Account account = getAccount(accountId);

        // Business logic in service layer - should be in Account entity
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(account.getAccountNumber(), amount, account.getBalance());
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setDescription("Withdrawal");
        transactionRepository.save(transaction);

        return accountRepository.save(account);
    }
}
