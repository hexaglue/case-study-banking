package com.acme.banking.core.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Card entity representing a payment card linked to an account.
 * Contains card lifecycle logic (issue, block, activate).
 *
 * @since 2.0.0
 */
public class Card {

    private CardId id;
    private String cardNumber;
    private LocalDate expiryDate;
    private String cvv;
    private AccountId accountId;
    private CardStatus status;
    private Money dailyLimit;

    private Card() {
    }

    /**
     * Issues a new card for an account.
     *
     * @param accountId  the account identifier
     * @param cardNumber the card number
     * @param expiryDate the expiry date
     * @param cvv        the CVV code
     * @param dailyLimit the daily spending limit
     * @return a new active card
     */
    public static Card issue(AccountId accountId, String cardNumber, LocalDate expiryDate, String cvv, Money dailyLimit) {
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(cardNumber, "cardNumber must not be null");
        Objects.requireNonNull(expiryDate, "expiryDate must not be null");
        Objects.requireNonNull(cvv, "cvv must not be null");
        Objects.requireNonNull(dailyLimit, "dailyLimit must not be null");
        Card card = new Card();
        card.accountId = accountId;
        card.cardNumber = cardNumber;
        card.expiryDate = expiryDate;
        card.cvv = cvv;
        card.dailyLimit = dailyLimit;
        card.status = CardStatus.ACTIVE;
        return card;
    }

    /**
     * Reconstitutes a card from persisted state.
     * <p>
     * This factory method restores a card without triggering any business logic
     * or validation. It is used by infrastructure mappers to rebuild domain objects
     * from the database.
     * </p>
     *
     * @param id         the card identifier
     * @param accountId  the account identifier
     * @param cardNumber the card number
     * @param expiryDate the expiry date
     * @param cvv        the CVV code
     * @param dailyLimit the daily spending limit
     * @param status     the current card status
     * @return a reconstituted card instance
     */
    public static Card reconstitute(
            CardId id,
            AccountId accountId,
            String cardNumber,
            LocalDate expiryDate,
            String cvv,
            Money dailyLimit,
            CardStatus status) {
        Card card = new Card();
        card.id = id;
        card.accountId = accountId;
        card.cardNumber = cardNumber;
        card.expiryDate = expiryDate;
        card.cvv = cvv;
        card.dailyLimit = dailyLimit;
        card.status = status;
        return card;
    }

    /**
     * Blocks this card.
     */
    public void block() {
        this.status = CardStatus.BLOCKED;
    }

    /**
     * Activates this card.
     */
    public void activate() {
        this.status = CardStatus.ACTIVE;
    }

    /**
     * Returns a masked card number showing only the last 4 digits.
     *
     * @return masked card number (e.g., "****-****-****-1234")
     */
    public String getMaskedNumber() {
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }

    public CardId getId() {
        return id;
    }

    public void setId(CardId id) {
        this.id = id;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public String getCvv() {
        return cvv;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public CardStatus getStatus() {
        return status;
    }

    public Money getDailyLimit() {
        return dailyLimit;
    }
}
