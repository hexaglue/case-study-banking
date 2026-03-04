package com.acme.banking.core.port.out;

import com.acme.banking.core.model.Transaction;
import com.acme.banking.core.model.Transfer;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

/**
 * Driven port for fraud detection operations.
 * <p>
 * Gateway to an external fraud detection service.
 * </p>
 *
 * @since 1.0.0
 */
@SecondaryPort
public interface FraudDetection {

    boolean checkTransfer(Transfer transfer);

    boolean checkTransaction(Transaction transaction);
}
