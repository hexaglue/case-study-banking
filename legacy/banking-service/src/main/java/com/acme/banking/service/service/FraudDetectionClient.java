package com.acme.banking.service.service;

import com.acme.banking.core.model.Transaction;
import com.acme.banking.core.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Mock client for external fraud detection service.
 * <p>
 * In a real application, this would call an external API for fraud detection.
 * This mock implementation always returns true (no fraud detected).
 * </p>
 * <p>
 * ANTI-PATTERN: External service integration as @Service in business layer
 * instead of being a proper driven port adapter. No abstraction or interface,
 * making testing and swapping implementations difficult.
 * </p>
 *
 * @since 1.0.0
 */
@Service
public class FraudDetectionClient {

    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionClient.class);

    /**
     * Checks if a transfer is potentially fraudulent.
     * <p>
     * Mock implementation always returns true (transfer is legitimate).
     * </p>
     *
     * @param transfer the transfer to check
     * @return true if transfer is legitimate, false if fraud detected
     */
    public boolean checkTransfer(Transfer transfer) {
        logger.info("Fraud check for transfer #{}: OK", transfer.getId());
        return true;
    }

    /**
     * Checks if a transaction is potentially fraudulent.
     * <p>
     * Mock implementation always returns true (transaction is legitimate).
     * </p>
     *
     * @param transaction the transaction to check
     * @return true if transaction is legitimate, false if fraud detected
     */
    public boolean checkTransaction(Transaction transaction) {
        logger.info("Fraud check for transaction #{}: OK", transaction.getId());
        return true;
    }
}
