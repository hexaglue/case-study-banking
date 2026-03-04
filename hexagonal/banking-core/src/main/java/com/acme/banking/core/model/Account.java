package com.acme.banking.core.model;

import com.acme.banking.core.exception.InsufficientFundsException;

import java.util.Objects;

/**
 * Account aggregate root representing a bank account.
 * Encapsulates deposit/withdraw business logic.
 *
 * @since 2.0.0
 */
public class Account {

    private AccountId id;
    private String accountNumber;
    private Money balance;
    private AccountType type;
    private CustomerId customerId;
    private boolean active;

    private Account() {
    }

    /**
     * Opens a new account.
     *
     * @param accountNumber the unique account number
     * @param type          the account type
     * @param customerId    the owner's identifier
     * @return a new active account with zero balance
     */
    public static Account open(String accountNumber, AccountType type, CustomerId customerId) {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.type = type;
        account.customerId = customerId;
        account.balance = Money.zero("EUR");
        account.active = true;
        return account;
    }

    /**
     * Reconstitutes an account from persisted state.
     * <p>
     * This factory method restores an account without triggering any business logic
     * or validation. It is used by infrastructure mappers to rebuild domain objects
     * from the database.
     * </p>
     *
     * @param id            the account identifier
     * @param accountNumber the unique account number
     * @param type          the account type
     * @param customerId    the owner's identifier
     * @param balance       the current balance
     * @param active        the active status
     * @return a reconstituted account instance
     */
    public static Account reconstitute(
            AccountId id, String accountNumber, AccountType type, CustomerId customerId, Money balance, boolean active) {
        Account account = new Account();
        account.id = id;
        account.accountNumber = accountNumber;
        account.type = type;
        account.customerId = customerId;
        account.balance = balance;
        account.active = active;
        return account;
    }

    /**
     * Deposits money into this account.
     *
     * @param amount the amount to deposit
     */
    public void deposit(Money amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        this.balance = this.balance.add(amount);
    }

    /**
     * Withdraws money from this account.
     *
     * @param amount the amount to withdraw
     * @throws InsufficientFundsException if the balance is insufficient
     */
    public void withdraw(Money amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (!this.balance.isGreaterThanOrEqual(amount)) {
            throw new InsufficientFundsException(
                    this.accountNumber, amount.amount(), this.balance.amount());
        }
        this.balance = this.balance.subtract(amount);
    }

    /**
     * Closes this account by marking it as inactive.
     */
    public void close() {
        this.active = false;
    }

    public AccountId getId() {
        return id;
    }

    public void setId(AccountId id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Money getBalance() {
        return balance;
    }

    public void setBalance(Money balance) {
        this.balance = balance;
    }

    public AccountType getType() {
        return type;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public boolean isActive() {
        return active;
    }
}
