package com.acme.banking.core.model;

import java.util.Objects;

/**
 * Typed identifier for {@link Account}.
 *
 * @since 2.0.0
 */
public record AccountId(Long value) {

    public AccountId {
        Objects.requireNonNull(value, "AccountId must not be null");
    }
}
