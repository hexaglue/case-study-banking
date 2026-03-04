package com.acme.banking.core.model;

import java.util.Objects;

/**
 * Value object representing an International Bank Account Number (IBAN).
 * Absorbs validation logic from the former {@code IbanUtils} utility class.
 *
 * @since 2.0.0
 */
public record Iban(String value) {

    public Iban {
        Objects.requireNonNull(value, "IBAN must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("IBAN must not be blank");
        }
        String cleaned = value.replaceAll("\\s", "");
        if (cleaned.length() < 15 || cleaned.length() > 34) {
            throw new IllegalArgumentException("IBAN length must be between 15 and 34 characters");
        }
        if (!Character.isLetter(cleaned.charAt(0)) || !Character.isLetter(cleaned.charAt(1))) {
            throw new IllegalArgumentException("IBAN must start with two letters");
        }
        if (!cleaned.substring(2).chars().allMatch(Character::isLetterOrDigit)) {
            throw new IllegalArgumentException("IBAN must contain only letters and digits after country code");
        }
        value = cleaned;
    }

    /**
     * Returns the IBAN formatted in groups of 4 characters.
     *
     * @return formatted IBAN (e.g., "FR76 1234 5678 9012 3456 7890 123")
     */
    public String formatted() {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(' ');
            }
            formatted.append(value.charAt(i));
        }
        return formatted.toString();
    }
}
