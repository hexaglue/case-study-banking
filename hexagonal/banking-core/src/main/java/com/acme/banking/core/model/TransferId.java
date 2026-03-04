package com.acme.banking.core.model;

import java.util.Objects;

/**
 * Typed identifier for {@link Transfer}.
 *
 * @since 2.0.0
 */
public record TransferId(Long value) {

    public TransferId {
        Objects.requireNonNull(value, "TransferId must not be null");
    }
}
