package com.acme.banking.core.port.in;

import com.acme.banking.core.model.AccountId;
import com.acme.banking.core.model.Card;
import com.acme.banking.core.model.CardId;
import com.acme.banking.core.model.Money;

import java.time.LocalDate;
import java.util.List;

/**
 * Driving port for card management use cases.
 *
 * @since 1.0.0
 */
public interface CardUseCases {

    Card issueCard(AccountId accountId, String cardNumber, LocalDate expiryDate, String cvv, Money dailyLimit);

    Card blockCard(CardId cardId);

    Card activateCard(CardId cardId);

    List<Card> getCardsByAccount(AccountId accountId);
}
