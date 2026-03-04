package com.acme.banking.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Beneficiary entity representing a saved transfer recipient.
 * Anti-patterns:
 * - JPA annotations on domain model
 * - Direct entity reference to Customer
 * - Primitive types (String) for IBAN/BIC instead of value objects
 * - No IBAN validation logic
 */
@Entity
@Table(name = "beneficiaries")
public class Beneficiary extends BaseEntity {

    @Column(nullable = false)
    private String name;

    // Anti-pattern: IBAN as primitive String instead of value object
    @Column(nullable = false)
    private String iban;

    private String bic;

    // Anti-pattern: Direct entity reference
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
}
