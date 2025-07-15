package com.inoka.inoka_app.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inoka.inoka_app.model.Card;
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
    public void handlePlayerReady(@Payload String playerId) {
        gameService.setPlayerReady(playerId);
    }

    @MessageMapping("/clashStart")
    public void handleClashStart(@Payload String gameId) {
        gameService.setClashStart(gameId);
    }

    @MessageMapping("/clashNew")
    public void handleNewClash(@Payload String gameId) {
        gameService.startNewClash(gameId);
    }

    @MessageMapping("/clashProcessed")
    public void handleClashProcessed(@Payload String gameId) {
        gameService.setClashFinishedProcessing(gameId);
    }

    @MessageMapping("/playCard")
    public void handlePlayCard(@Payload Map<String, Object> payload) {
        ObjectMapper objectMapper = new ObjectMapper();
        String playerId = (String) payload.get("playerId");
        Card card = objectMapper.convertValue(payload.get("card"), Card.class);
    
        gameService.putCardInPlay(playerId, card);
    }

    @MessageMapping("/clashAction")
    public void handleClashAction(@Payload Map<String, Object> payload) {
        String dealingPlayerId = (String) payload.get("userId");
        String receivingPlayerId = (String) payload.get("targetId");
        
        gameService.resolveClashAction(dealingPlayerId, receivingPlayerId);
    }

    @MessageMapping("/gotKnockout")
    public void handlePlayerPickUpKnockout(@Payload String playerId) {
        gameService.playerPickUpKnockout(playerId);
    }

    @MessageMapping("/clashForfeit")
    public void handlePlayerForfeitClash(@Payload String playerId) {
        gameService.playerForfeitClash(playerId);
    }
}
