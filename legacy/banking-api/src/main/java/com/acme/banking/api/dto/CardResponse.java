package com.acme.banking.api.dto;

import com.acme.banking.core.model.Card;
import com.acme.banking.core.model.CardStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CardResponse(
    Long id,
    String cardNumberMasked,
    LocalDate expiryDate,
    CardStatus status,
    BigDecimal dailyLimit,
    Long accountId
) {
    public static CardResponse from(Card card) {
        String masked = "****-****-****-" + card.getCardNumber().substring(card.getCardNumber().length() - 4);
        return new CardResponse(
            card.getId(),
            masked,
            card.getExpiryDate(),
            card.getStatus(),
            card.getDailyLimit(),
            card.getAccount().getId()
        );
    }
}
