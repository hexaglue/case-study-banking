package com.acme.banking.service.service;

import com.acme.banking.core.model.Customer;
import com.acme.banking.persistence.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling all customer-related operations.
 * <p>
 * ANTI-PATTERN: Simple CRUD service that adds little value beyond repository operations.
 * In a proper domain model, customer-related business rules would be encapsulated in
 * the Customer entity itself.
 * </p>
 *
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * Creates a new CustomerService with required dependencies.
     *
     * @param customerRepository repository for customer operations
     */
    public CustomerService(CustomerRepository customerRepository) {
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
    @Transactional
    public Customer createCustomer(String firstName, String lastName, String email, String phone) {
        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
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
    @Transactional
    public Customer updateCustomer(Long id, String firstName, String lastName, String email, String phone) {
        Customer customer = getCustomer(id);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        customer.setPhone(phone);
        return customerRepository.save(customer);
    }

    /**
     * Gets a customer by ID.
     *
     * @param id the customer ID
     * @return the customer
     * @throws RuntimeException if customer not found
     */
    public Customer getCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
    }

    /**
     * Gets a customer by email address.
     *
     * @param email the email address
     * @return the customer, or null if not found
     */
    public Customer getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email).orElse(null);
    }
}
