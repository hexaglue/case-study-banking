package com.acme.banking.core.exception;

/**
 * Exception thrown when an account cannot be found.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long id) {
        super(String.format("Account with ID %d not found", id));
    }

    public AccountNotFoundException(String accountNumber) {
        super(String.format("Account with number %s not found", accountNumber));
    }

    public AccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
