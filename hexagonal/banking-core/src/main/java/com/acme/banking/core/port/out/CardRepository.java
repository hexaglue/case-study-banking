package com.acme.banking.core.port.out;

import com.acme.banking.core.model.AccountId;
import com.acme.banking.core.model.Card;
import com.acme.banking.core.model.CardId;

import java.util.List;
import java.util.Optional;

/**
 * Driven port for card persistence operations.
 *
 * @since 1.0.0
 */
public interface CardRepository {

    Card save(Card card);

    Optional<Card> findById(CardId id);

    List<Card> findByAccountId(AccountId accountId);

    Optional<Card> findByCardNumber(String cardNumber);
}
