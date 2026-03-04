package com.acme.banking.core.model;

import java.util.Objects;

/**
 * Typed identifier for {@link Beneficiary}.
 *
 * @since 2.0.0
 */
public record BeneficiaryId(Long value) {

    public BeneficiaryId {
        Objects.requireNonNull(value, "BeneficiaryId must not be null");
    }
}
