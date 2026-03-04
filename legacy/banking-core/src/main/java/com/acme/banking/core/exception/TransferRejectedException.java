package com.acme.banking.core.exception;

/**
 * Exception thrown when a transfer is rejected.
 */
public class TransferRejectedException extends RuntimeException {

    private final Long transferId;
    private final String reason;

    public TransferRejectedException(Long transferId, String reason) {
        super(String.format("Transfer %d rejected: %s", transferId, reason));
        this.transferId = transferId;
        this.reason = reason;
    }

    public Long getTransferId() {
        return transferId;
    }

    public String getReason() {
        return reason;
    }
}
