package com.acme.banking.core.port.out;

import com.acme.banking.core.model.Beneficiary;
import com.acme.banking.core.model.CustomerId;

import java.util.List;

/**
 * Driven port for beneficiary persistence operations.
 *
 * @since 1.0.0
 */
public interface BeneficiaryRepository {

    Beneficiary save(Beneficiary beneficiary);

    List<Beneficiary> findByCustomerId(CustomerId customerId);
}
