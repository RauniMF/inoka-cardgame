package com.inoka.inoka_app.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import com.inoka.inoka_app.model.Game;
import com.inoka.inoka_app.service.GameService;

@RestController
public class GameWebSocketController {
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;
    
    public GameWebSocketController(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/updateGame") // Client sends message here
    public void updateGame(String gameId, SimpMessageHeaderAccessor headerAccessor) {
        Game game = gameService.getGame(gameId);

        String sessionId = headerAccessor.getSessionId();

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/game", game);
    }
}
