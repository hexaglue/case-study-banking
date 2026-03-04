package com.acme.banking.service.application;

import com.acme.banking.core.exception.AccountNotFoundException;
import com.acme.banking.core.model.AccountId;
import com.acme.banking.core.model.Card;
import com.acme.banking.core.model.CardId;
import com.acme.banking.core.model.Money;
import com.acme.banking.core.port.in.CardUseCases;
import com.acme.banking.core.port.out.AccountRepository;
import com.acme.banking.core.port.out.CardRepository;
import com.acme.banking.core.port.out.NotificationSender;

import java.time.LocalDate;
import java.util.List;

/**
 * Application service handling card management operations.
 *
 * @since 1.0.0
 */
public class CardApplicationService implements CardUseCases {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final NotificationSender notificationSender;

    /**
     * Creates a new CardApplicationService with required dependencies.
     *
     * @param cardRepository     repository for card operations
     * @param accountRepository  repository for account operations
     * @param notificationSender notification sender
     */
    public CardApplicationService(
            CardRepository cardRepository, AccountRepository accountRepository, NotificationSender notificationSender) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.notificationSender = notificationSender;
    }

    /**
     * Issues a new card for an account.
     *
     * @param accountId  the account ID
     * @param cardNumber the card number
     * @param expiryDate the expiry date
     * @param cvv        the CVV code
     * @param dailyLimit the daily spending limit
     * @return the created card
     * @throws AccountNotFoundException if account not found
     */
    @Override
    public Card issueCard(AccountId accountId, String cardNumber, LocalDate expiryDate, String cvv, Money dailyLimit) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.value()));

        Card card = Card.issue(accountId, cardNumber, expiryDate, cvv, dailyLimit);
        return cardRepository.save(card);
    }

    /**
     * Blocks a card.
     *
     * @param cardId the card ID
     * @return the blocked card
     * @throws RuntimeException if card not found
     */
    @Override
    public Card blockCard(CardId cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found: " + cardId.value()));
        card.block();
        Card saved = cardRepository.save(card);
        notificationSender.sendCardAlert(saved, "Card blocked");
        return saved;
    }

    /**
     * Activates a card.
     *
     * @param cardId the card ID
     * @return the activated card
     * @throws RuntimeException if card not found
     */
    @Override
    public Card activateCard(CardId cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found: " + cardId.value()));
        card.activate();
        return cardRepository.save(card);
    }

    /**
     * Gets all cards for an account.
     *
     * @param accountId the account ID
     * @return list of cards
     */
    @Override
    public List<Card> getCardsByAccount(AccountId accountId) {
        return cardRepository.findByAccountId(accountId);
    }
}
