package com.inoka.inoka_app.controller;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.RestController;

import com.inoka.inoka_app.model.Card;
import com.inoka.inoka_app.model.Game;
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
    public void handlePlayerReady(Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized attempt to mark player ready");
            return;
        }
        
        String authenticatedUserId = principal.getName();
        gameService.setPlayerReady(authenticatedUserId);
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
    public void handlePlayCard(@Payload Card card, Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized card play attempt");
            return;
        }
        
        String playerId = principal.getName();
        gameService.putCardInPlay(playerId, card);
    }

    @MessageMapping("/clashAction")
    public void handleClashAction(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized clash action attempt");
            return;
        }
        
        String dealingPlayerId = principal.getName();  // Derive from Principal
        
        // Client now sends target SEAT, not target UUID
        Integer targetSeat = (Integer) payload.get("targetSeat");
        
        // Look up target player ID from seat in the game
        Optional<Game> gameOpt = gameService.getGameByPlayerId(dealingPlayerId);
        if (gameOpt.isEmpty()) {
            logger.warn("Player {} not in any game", dealingPlayerId);
            return;
        }
        
        Game game = gameOpt.get();
        Optional<String> receivingPlayerIdOpt = Optional.empty();
        if (targetSeat != -1) game.getPlayerIdBySeat(targetSeat);
        
        // targetSeat == -1: receivingPlayerId = "null"
        if (receivingPlayerIdOpt.isEmpty() && targetSeat != -1) {
            logger.warn("Invalid target seat: {}", targetSeat);
            return;
        }
        
        /*
         *  resolveClashAction() -> dealingPlayer damages receivingPlayer
         *  if receivingPlayerId == "null", dealingPlayer skips turn
         */
        gameService.resolveClashAction(
            dealingPlayerId,
            receivingPlayerIdOpt.isPresent() ? receivingPlayerIdOpt.get()
            : "null"
        );
    }

    @MessageMapping("/gotKnockout")
    public void handlePlayerPickUpKnockout(Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized knockout attempt");
            return;
        }
        
        String playerId = principal.getName();
        gameService.playerPickUpKnockout(playerId);
    }

    @MessageMapping("/clashForfeit")
    public void handlePlayerForfeitClash(Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized forfeit attempt");
            return;
        }
        
        String playerId = principal.getName();
        gameService.playerForfeitClash(playerId);
    }
}
