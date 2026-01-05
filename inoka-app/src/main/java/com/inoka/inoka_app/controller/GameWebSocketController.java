package com.inoka.inoka_app.controller;

import java.security.Principal;
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
            Principal user = headerAccessor.getUser();
            if (user != null) {
                logger.info("Player " + user.getName() + " subscribed to: " + destination);
            }
        }
    }

    @MessageMapping("/playerReady")
    public void handlePlayerReady(@Payload String playerId, Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized attempt to mark player ready");
            return;
        }
        
        String authenticatedUserId = principal.getName();
        if (!authenticatedUserId.equals(playerId)) {
            logger.warn("Player " + authenticatedUserId + " attempting to modify player " + playerId);
            return;
        }
        
        gameService.setPlayerReady(playerId);
    }

    @MessageMapping("/clashStart")
    public void handleClashStart(@Payload String gameId, Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized clash start attempt");
            return;
        }
        gameService.setClashStart(gameId);
    }

    @MessageMapping("/clashNew")
    public void handleNewClash(@Payload String gameId, Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized new clash attempt");
            return;
        }
        gameService.startNewClash(gameId);
    }

    @MessageMapping("/clashProcessed")
    public void handleClashProcessed(@Payload String gameId, Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized clash processed attempt");
            return;
        }
        gameService.setClashFinishedProcessing(gameId);
    }

    @MessageMapping("/playCard")
    public void handlePlayCard(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized card play attempt");
            return;
        }
        
        String playerId = (String) payload.get("playerId");
        String authenticatedUserId = principal.getName();
        
        if (!authenticatedUserId.equals(playerId)) {
            logger.warn("Player " + authenticatedUserId + " attempting to play cards for player " + playerId);
            return;
        }
        
        ObjectMapper objectMapper = new ObjectMapper();
        Card card = objectMapper.convertValue(payload.get("card"), Card.class);
        gameService.putCardInPlay(playerId, card);
    }

    @MessageMapping("/clashAction")
    public void handleClashAction(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized clash action attempt");
            return;
        }
        
        String dealingPlayerId = (String) payload.get("userId");
        String authenticatedUserId = principal.getName();
        
        if (!authenticatedUserId.equals(dealingPlayerId)) {
            logger.warn("Player " + authenticatedUserId + " attempting to perform action as player " + dealingPlayerId);
            return;
        }
        
        String receivingPlayerId = (String) payload.get("targetId");
        gameService.resolveClashAction(dealingPlayerId, receivingPlayerId);
    }

    @MessageMapping("/gotKnockout")
    public void handlePlayerPickUpKnockout(@Payload String playerId, Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized knockout attempt");
            return;
        }
        
        String authenticatedUserId = principal.getName();
        if (!authenticatedUserId.equals(playerId)) {
            logger.warn("Player " + authenticatedUserId + " attempting to pickup knockout for player " + playerId);
            return;
        }
        
        gameService.playerPickUpKnockout(playerId);
    }

    @MessageMapping("/clashForfeit")
    public void handlePlayerForfeitClash(@Payload String playerId, Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized forfeit attempt");
            return;
        }
        
        String authenticatedUserId = principal.getName();
        if (!authenticatedUserId.equals(playerId)) {
            logger.warn("Player " + authenticatedUserId + " attempting to forfeit for player " + playerId);
            return;
        }
        
        gameService.playerForfeitClash(playerId);
    }
}
