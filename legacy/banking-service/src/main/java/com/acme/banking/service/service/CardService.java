package com.acme.banking.service.service;

import com.acme.banking.core.exception.AccountNotFoundException;
import com.acme.banking.core.model.Account;
import com.acme.banking.core.model.Card;
import com.acme.banking.core.model.CardStatus;
import com.acme.banking.persistence.repository.AccountRepository;
import com.acme.banking.persistence.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service handling card management operations.
 * <p>
 * ANTI-PATTERN: Simple CRUD service with business logic scattered between
 * service and repository layers. Card business rules should be in the Card entity.
 * </p>
 *
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;

    /**
     * Creates a new CardService with required dependencies.
     *
     * @param cardRepository    repository for card operations
     * @param accountRepository repository for account operations
     */
    public CardService(CardRepository cardRepository, AccountRepository accountRepository) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
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
    @Transactional
    public Card issueCard(Long accountId, String cardNumber, LocalDate expiryDate, String cvv, BigDecimal dailyLimit) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        Card card = new Card();
        card.setAccount(account);
        card.setCardNumber(cardNumber);
        card.setExpiryDate(expiryDate);
        card.setCvv(cvv);
        card.setDailyLimit(dailyLimit);
        card.setStatus(CardStatus.ACTIVE);

        return cardRepository.save(card);
    }

    /**
     * Blocks a card.
     *
     * @param cardId the card ID
     * @return the blocked card
     * @throws RuntimeException if card not found
     */
    @Transactional
    public Card blockCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));
        card.setStatus(CardStatus.BLOCKED);
        return cardRepository.save(card);
    }

    /**
     * Activates a card.
     *
     * @param cardId the card ID
     * @return the activated card
     * @throws RuntimeException if card not found
     */
    @Transactional
    public Card activateCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));
        card.setStatus(CardStatus.ACTIVE);
        return cardRepository.save(card);
    }

    /**
     * Gets all cards for an account.
     *
     * @param accountId the account ID
     * @return list of cards
     */
    public List<Card> getCardsByAccount(Long accountId) {
        return cardRepository.findByAccountId(accountId);
    }
}
