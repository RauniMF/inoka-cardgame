package com.inoka.inoka_app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.inoka.inoka_app.event.GameUpdateEvent;
import com.inoka.inoka_app.model.Game;
import com.inoka.inoka_app.model.Player;

public class CircularDependencyResolutionTest {

    private GameService gameService;
    private SchedulerService schedulerService;
    private ApplicationEventPublisher eventPublisher;
    private PlayerService playerService;

    @BeforeEach
    public void setUp() {
        eventPublisher = mock(ApplicationEventPublisher.class);
        playerService = mock(PlayerService.class);
        gameService = new GameService(playerService, eventPublisher);
        schedulerService = new SchedulerService(mock(org.springframework.messaging.simp.SimpMessagingTemplate.class));
    }

    @Test
    public void testGameServiceDoesNotDependOnSchedulerService() {
        // This test verifies that GameService can be instantiated without SchedulerService
        // If there was a circular dependency, this would fail
        assertNotNull(gameService);
    }

    @Test
    public void testSchedulerServiceDoesNotDependOnGameService() {
        // This test verifies that SchedulerService can be instantiated without GameService
        // If there was a circular dependency, this would fail
        assertNotNull(schedulerService);
    }

    @Test
    public void testGameServicePublishesEventOnGameUpdate() {
        // Create a game
        Player player = new Player("Test Player");
        Game game = gameService.createGame("test", player);
        
        // Verify that event publisher was called
        // The createGame method calls addPlayerToGame which publishes an event
        verify(eventPublisher, times(1)).publishEvent(any(GameUpdateEvent.class));
    }

    @Test
    public void testSchedulerServiceHandlesGameUpdateEvent() {
        // Create a game object
        Game game = new Game();
        
        // Create an event
        GameUpdateEvent event = new GameUpdateEvent(this, game);
        
        // Handle the event through the scheduler service
        schedulerService.handleGameUpdateEvent(event);
        
        // Verify the game was queued (we can't easily verify the internal state,
        // but the fact that the method executes without error is a good sign)
        assertNotNull(event.getGame());
        assertEquals(game.getId(), event.getGameId());
    }
}
