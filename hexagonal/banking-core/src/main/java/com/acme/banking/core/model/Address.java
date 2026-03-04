package com.acme.banking.core.model;

import java.util.Objects;

/**
 * Value object representing a postal address.
 *
 * @since 2.0.0
 */
public record Address(String street, String city, String zipCode, String country) {

    public Address {
        Objects.requireNonNull(street, "street must not be null");
        Objects.requireNonNull(city, "city must not be null");
        Objects.requireNonNull(zipCode, "zipCode must not be null");
        Objects.requireNonNull(country, "country must not be null");
        if (street.isBlank()) throw new IllegalArgumentException("street must not be blank");
        if (city.isBlank()) throw new IllegalArgumentException("city must not be blank");
        if (zipCode.isBlank()) throw new IllegalArgumentException("zipCode must not be blank");
        if (country.isBlank()) throw new IllegalArgumentException("country must not be blank");
    }
}
