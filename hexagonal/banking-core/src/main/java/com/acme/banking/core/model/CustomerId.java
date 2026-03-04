package com.acme.banking.core.model;

import java.util.Objects;

/**
 * Typed identifier for {@link Customer}.
 *
 * @since 2.0.0
 */
public record CustomerId(Long value) {

    public CustomerId {
        Objects.requireNonNull(value, "CustomerId must not be null");
    }
}
