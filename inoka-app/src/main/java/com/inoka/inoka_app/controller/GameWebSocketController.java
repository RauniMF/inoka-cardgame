package com.inoka.inoka_app.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.RestController;

import com.inoka.inoka_app.service.GameService;

@RestController
public class GameWebSocketController {
    private final GameService gameService;
    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketController.class);
    
    public GameWebSocketController(GameService gameService) {
        this.gameService = gameService;
    }

    @EventListener
    public void handleSubscriptionEvent(Message<?> event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event);
        if (headerAccessor.getCommand() == StompCommand.SUBSCRIBE) {
            String destination = headerAccessor.getDestination();
            logger.info("Player subscribed to: " + destination);
        }
    }

    @MessageMapping("/playerReady") // Client sends message here
    public void handlePlayerReady(String playerId) {
        gameService.setPlayerReady(playerId);
    }
}
