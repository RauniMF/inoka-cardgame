package com.inoka.inoka_app.event;

import java.util.List;

import org.springframework.context.ApplicationEvent;

import com.inoka.inoka_app.model.Card;

public class DeckUpdateEvent extends ApplicationEvent {
    
    private final String playerId;
    private final List<Card> deck;

    public DeckUpdateEvent(Object source, String playerId, List<Card> deck) {
        super(source);
        this.playerId = playerId;
        this.deck = deck;
    }

    public List<Card> getDeck() {
        return this.deck;
    }

    public String getPlayerId() {
        return this.playerId;
    }
}
