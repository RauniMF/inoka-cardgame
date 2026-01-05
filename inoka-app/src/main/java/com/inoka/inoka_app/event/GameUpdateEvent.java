package com.inoka.inoka_app.event;

import org.springframework.context.ApplicationEvent;

import com.inoka.inoka_app.model.Game;

public class GameUpdateEvent extends ApplicationEvent {
    
    private final Game game;

    public GameUpdateEvent(Object source, Game game) {
        super(source);
        this.game = game;
    }

    public Game getGame() {
        return game;
    }
    
    public String getGameId() {
        return game.getId();
    }
}
