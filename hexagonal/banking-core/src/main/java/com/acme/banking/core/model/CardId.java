package com.acme.banking.core.model;

import java.util.Objects;

/**
 * Typed identifier for {@link Card}.
 *
 * @since 2.0.0
 */
public record CardId(Long value) {

    public CardId {
        Objects.requireNonNull(value, "CardId must not be null");
    }
}
