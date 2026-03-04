package com.acme.banking.core.port.in;

import com.acme.banking.core.model.Customer;
import com.acme.banking.core.model.CustomerId;
import com.acme.banking.core.model.Email;

/**
 * Driving port for customer management use cases.
 *
 * @since 1.0.0
 */
public interface CustomerUseCases {

    Customer createCustomer(String firstName, String lastName, Email email, String phone);

    Customer updateCustomer(CustomerId id, String firstName, String lastName, Email email, String phone);

    Customer getCustomer(CustomerId id);

    Customer getCustomerByEmail(Email email);
}
