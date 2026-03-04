package com.acme.banking.core.port.out;

import com.acme.banking.core.model.Customer;
import com.acme.banking.core.model.CustomerId;
import com.acme.banking.core.model.Email;

import java.util.Optional;

/**
 * Driven port for customer persistence operations.
 *
 * @since 1.0.0
 */
public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(CustomerId id);

    Optional<Customer> findByEmail(Email email);

    boolean existsByEmail(Email email);
}
