package com.acme.banking.service.service;

import com.acme.banking.core.model.Card;
import com.acme.banking.core.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Mock service for sending notifications.
 * <p>
 * In a real application, this would integrate with email/SMS providers.
 * This mock implementation just logs notification events.
 * </p>
 * <p>
 * ANTI-PATTERN: Infrastructure concern (notification) mixed into service layer
 * instead of being a proper driven port adapter.
 * </p>
 *
 * @since 1.0.0
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    /**
     * Sends a notification for a completed transfer.
     *
     * @param transfer the completed transfer
     */
    public void sendTransferNotification(Transfer transfer) {
        logger.info("Transfer notification sent for transfer #{}", transfer.getId());
    }

    /**
     * Sends an alert notification for a card.
     *
     * @param card    the card
     * @param message the alert message
     */
    public void sendCardAlert(Card card, String message) {
        String cardNumber = card.getCardNumber();
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        logger.info("Card alert for card ending {}: {}", last4, message);
    }
}
