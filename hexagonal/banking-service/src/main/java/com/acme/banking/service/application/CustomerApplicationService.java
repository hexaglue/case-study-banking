package com.acme.banking.service.application;

import com.acme.banking.core.model.Customer;
import com.acme.banking.core.model.CustomerId;
import com.acme.banking.core.model.Email;
import com.acme.banking.core.port.in.CustomerUseCases;
import com.acme.banking.core.port.out.CustomerRepository;

/**
 * Application service handling all customer-related operations.
 *
 * @since 1.0.0
 */
public class CustomerApplicationService implements CustomerUseCases {

    private final CustomerRepository customerRepository;

    /**
     * Creates a new CustomerApplicationService with required dependencies.
     *
     * @param customerRepository repository for customer operations
     */
    public CustomerApplicationService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Creates a new customer.
     *
     * @param firstName the customer's first name
     * @param lastName  the customer's last name
     * @param email     the customer's email address
     * @param phone     the customer's phone number
     * @return the created customer
     */
    @Override
    public Customer createCustomer(String firstName, String lastName, Email email, String phone) {
        Customer customer = Customer.create(firstName, lastName, email);
        customer.setPhone(phone);
        return customerRepository.save(customer);
    }

    /**
     * Updates an existing customer.
     *
     * @param id        the customer ID
     * @param firstName the customer's first name
     * @param lastName  the customer's last name
     * @param email     the customer's email address
     * @param phone     the customer's phone number
     * @return the updated customer
     * @throws RuntimeException if customer not found
     */
    @Override
    public Customer updateCustomer(CustomerId id, String firstName, String lastName, Email email, String phone) {
        Customer customer = getCustomer(id);
        customer.updateProfile(firstName, lastName, email, phone);
        return customerRepository.save(customer);
    }

    /**
     * Gets a customer by ID.
     *
     * @param id the customer ID
     * @return the customer
     * @throws RuntimeException if customer not found
     */
    @Override
    public Customer getCustomer(CustomerId id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id.value()));
    }

    /**
     * Gets a customer by email address.
     *
     * @param email the email address
     * @return the customer, or null if not found
     */
    @Override
    public Customer getCustomerByEmail(Email email) {
        return customerRepository.findByEmail(email).orElse(null);
    }
}
