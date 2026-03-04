package com.acme.banking.core.model;

import java.util.Objects;

/**
 * Value object representing an email address.
 *
 * @since 2.0.0
 */
public record Email(String value) {

    public Email {
        Objects.requireNonNull(value, "email must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (!value.contains("@")) {
            throw new IllegalArgumentException("email must contain '@'");
        }
    }
}
