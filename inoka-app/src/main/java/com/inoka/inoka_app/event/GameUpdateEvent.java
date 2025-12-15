package com.inoka.inoka_app.event;

import org.springframework.context.ApplicationEvent;

import com.inoka.inoka_app.model.Game;

/**
 * Event published when a game state changes and needs to be broadcasted to clients.
 * This event decouples GameService from SchedulerService, breaking the circular dependency.
 * The event contains the full game object to avoid SchedulerService needing to query GameService.
 */
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
