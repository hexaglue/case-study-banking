package com.acme.banking.core.model;

import java.util.Objects;

/**
 * Customer aggregate representing a bank customer.
 * Pure domain class with no infrastructure dependencies.
 *
 * @since 2.0.0
 */
public class Customer {

    private CustomerId id;
    private String firstName;
    private String lastName;
    private Email email;
    private String phone;
    private Address address;

    private Customer() {
    }

    /**
     * Creates a new customer.
     *
     * @param firstName the customer's first name
     * @param lastName  the customer's last name
     * @param email     the customer's email
     * @return a new customer instance
     */
    public static Customer create(String firstName, String lastName, Email email) {
        Objects.requireNonNull(firstName, "firstName must not be null");
        Objects.requireNonNull(lastName, "lastName must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Customer customer = new Customer();
        customer.firstName = firstName;
        customer.lastName = lastName;
        customer.email = email;
        return customer;
    }

    /**
     * Reconstitutes a customer from persisted state.
     * <p>
     * This factory method restores a customer without triggering any business logic
     * or validation. It is used by infrastructure mappers to rebuild domain objects
     * from the database.
     * </p>
     *
     * @param id        the customer identifier
     * @param firstName the customer's first name
     * @param lastName  the customer's last name
     * @param email     the customer's email
     * @param phone     the customer's phone number
     * @param address   the customer's address
     * @return a reconstituted customer instance
     */
    public static Customer reconstitute(
            CustomerId id, String firstName, String lastName, Email email, String phone, Address address) {
        Customer customer = new Customer();
        customer.id = id;
        customer.firstName = firstName;
        customer.lastName = lastName;
        customer.email = email;
        customer.phone = phone;
        customer.address = address;
        return customer;
    }

    public void updateProfile(String firstName, String lastName, Email email, String phone) {
        this.firstName = Objects.requireNonNull(firstName, "firstName must not be null");
        this.lastName = Objects.requireNonNull(lastName, "lastName must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.phone = phone;
    }

    public void updateAddress(Address address) {
        this.address = address;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public CustomerId getId() {
        return id;
    }

    public void setId(CustomerId id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Email getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Address getAddress() {
        return address;
    }
}
