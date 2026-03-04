package com.acme.banking.service.adapter.out;

import com.acme.banking.core.model.Transaction;
import com.acme.banking.core.model.Transfer;
import com.acme.banking.core.port.out.FraudDetection;
import java.util.logging.Logger;
import org.springframework.stereotype.Component;

/**
 * Stub implementation of the {@link FraudDetection} driven port.
 * <p>
 * Logs all fraud checks and always returns a non-fraudulent result.
 * In a real application, this would integrate with an external fraud detection service.
 * </p>
 *
 * @since 1.0.0
 */
@Component
public class FraudDetectionAdapter implements FraudDetection {

    private static final Logger LOG = Logger.getLogger(FraudDetectionAdapter.class.getName());

    @Override
    public boolean checkTransfer(Transfer transfer) {
        LOG.info(() -> String.format("Fraud check for transfer %s: amount=%s, from=%s to=%s — PASSED",
                transfer.getId().value(),
                transfer.getAmount().amount(),
                transfer.getSourceAccountId().value(),
                transfer.getTargetAccountId().value()));
        return true;
    }

    @Override
    public boolean checkTransaction(Transaction transaction) {
        LOG.info(() -> String.format("Fraud check for transaction %s: amount=%s — PASSED",
                transaction.getId().value(),
                transaction.getAmount().amount()));
        return true;
    }
}
