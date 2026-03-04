package com.acme.banking.core.model;

import java.util.Objects;

/**
 * Beneficiary entity representing a saved transfer recipient.
 *
 * @since 2.0.0
 */
public class Beneficiary {

    private BeneficiaryId id;
    private String name;
    private Iban iban;
    private String bic;
    private CustomerId customerId;

    private Beneficiary() {
    }

    /**
     * Creates a new beneficiary.
     *
     * @param customerId the owner's customer identifier
     * @param name       the beneficiary name
     * @param iban       the beneficiary IBAN
     * @param bic        the BIC/SWIFT code
     * @return a new beneficiary
     */
    public static Beneficiary create(CustomerId customerId, String name, Iban iban, String bic) {
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(iban, "iban must not be null");
        Beneficiary b = new Beneficiary();
        b.customerId = customerId;
        b.name = name;
        b.iban = iban;
        b.bic = bic;
        return b;
    }

    /**
     * Reconstitutes a beneficiary from persisted state.
     * <p>
     * This factory method restores a beneficiary without triggering any business logic
     * or validation. It is used by infrastructure mappers to rebuild domain objects
     * from the database.
     * </p>
     *
     * @param id         the beneficiary identifier
     * @param customerId the owner's customer identifier
     * @param name       the beneficiary name
     * @param iban       the beneficiary IBAN
     * @param bic        the BIC/SWIFT code
     * @return a reconstituted beneficiary instance
     */
    public static Beneficiary reconstitute(BeneficiaryId id, CustomerId customerId, String name, Iban iban, String bic) {
        Beneficiary b = new Beneficiary();
        b.id = id;
        b.customerId = customerId;
        b.name = name;
        b.iban = iban;
        b.bic = bic;
        return b;
    }

    public BeneficiaryId getId() {
        return id;
    }

    public void setId(BeneficiaryId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Iban getIban() {
        return iban;
    }

    public String getBic() {
        return bic;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }
}
