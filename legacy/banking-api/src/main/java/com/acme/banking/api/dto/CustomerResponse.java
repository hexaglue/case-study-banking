package com.acme.banking.api.dto;

import com.acme.banking.core.model.Customer;

public record CustomerResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    String phone,
    String city,
    String country
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
            customer.getId(),
            customer.getFirstName(),
            customer.getLastName(),
            customer.getEmail(),
            customer.getPhone(),
            customer.getCity(),
            customer.getCountry()
        );
    }
}
