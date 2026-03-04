package com.acme.banking.service.adapter.out;

import com.acme.banking.core.model.Card;
import com.acme.banking.core.model.Transfer;
import com.acme.banking.core.port.out.NotificationSender;
import java.util.logging.Logger;
import org.springframework.stereotype.Component;

/**
 * Stub implementation of the {@link NotificationSender} driven port.
 * <p>
 * Logs all notifications. In a real application, this would send emails,
 * SMS, or push notifications through an external service.
 * </p>
 *
 * @since 1.0.0
 */
@Component
public class NotificationSenderAdapter implements NotificationSender {

    private static final Logger LOG = Logger.getLogger(NotificationSenderAdapter.class.getName());

    @Override
    public void sendTransferNotification(Transfer transfer) {
        LOG.info(() -> String.format("Notification sent for transfer %s: %s from account %s to account %s",
                transfer.getId().value(),
                transfer.getAmount().amount(),
                transfer.getSourceAccountId().value(),
                transfer.getTargetAccountId().value()));
    }

    @Override
    public void sendCardAlert(Card card, String message) {
        LOG.info(() -> String.format("Card alert for card %s: %s",
                card.getId().value(),
                message));
    }
}
