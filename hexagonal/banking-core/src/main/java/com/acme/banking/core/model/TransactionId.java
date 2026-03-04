package com.acme.banking.core.model;

import java.util.Objects;

/**
 * Typed identifier for {@link Transaction}.
 *
 * @since 2.0.0
 */
public record TransactionId(Long value) {

    public TransactionId {
        Objects.requireNonNull(value, "TransactionId must not be null");
    }
}
