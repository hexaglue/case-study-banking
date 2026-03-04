package com.acme.banking.api.controller;

import com.acme.banking.api.dto.CardResponse;
import com.acme.banking.core.model.Card;
import com.acme.banking.service.service.CardService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse issueCard(@RequestBody Map<String, Object> request) {
        Long accountId = ((Number) request.get("accountId")).longValue();
        String cardNumber = (String) request.get("cardNumber");
        String expiryDate = (String) request.get("expiryDate");
        String cvv = (String) request.get("cvv");
        BigDecimal dailyLimit = new BigDecimal(request.get("dailyLimit").toString());

        Card card = cardService.issueCard(accountId, cardNumber,
                java.time.LocalDate.parse(expiryDate), cvv, dailyLimit);
        return CardResponse.from(card);
    }

    @PostMapping("/{id}/block")
    public CardResponse blockCard(@PathVariable Long id) {
        Card card = cardService.blockCard(id);
        return CardResponse.from(card);
    }

    @PostMapping("/{id}/activate")
    public CardResponse activateCard(@PathVariable Long id) {
        Card card = cardService.activateCard(id);
        return CardResponse.from(card);
    }

    @GetMapping("/account/{accountId}")
    public List<CardResponse> getCardsByAccount(@PathVariable Long accountId) {
        List<Card> cards = cardService.getCardsByAccount(accountId);
        return cards.stream()
            .map(CardResponse::from)
            .toList();
    }
}
