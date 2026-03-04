package com.acme.banking.core.port.out;

import com.acme.banking.core.model.Card;
import com.acme.banking.core.model.Transfer;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

/**
 * Driven port for sending notifications.
 * <p>
 * Event publisher abstraction for notification infrastructure.
 * </p>
 *
 * @since 1.0.0
 */
@SecondaryPort
public interface NotificationSender {

    void sendTransferNotification(Transfer transfer);

    void sendCardAlert(Card card, String message);
}
