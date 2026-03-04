package com.acme.banking.service.event;

import com.acme.banking.core.model.Transfer;
import org.springframework.context.ApplicationEvent;

/**
 * Spring ApplicationEvent triggered when a transfer is completed.
 * <p>
 * ANTI-PATTERN: Using Spring-specific event mechanism instead of domain events.
 * This couples domain logic to Spring Framework infrastructure.
 * </p>
 *
 * @since 1.0.0
 */
public class TransferCompletedEvent extends ApplicationEvent {

    private final Transfer transfer;

    /**
     * Creates a new transfer completed event.
     *
     * @param source   the source object that triggered the event
     * @param transfer the completed transfer
     */
    public TransferCompletedEvent(Object source, Transfer transfer) {
        super(source);
        this.transfer = transfer;
    }

    /**
     * Gets the completed transfer.
     *
     * @return the transfer that was completed
     */
    public Transfer getTransfer() {
        return transfer;
    }
}
