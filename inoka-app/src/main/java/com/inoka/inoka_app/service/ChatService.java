package com.inoka.inoka_app.service;

import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.inoka.inoka_app.model.ChatMessage;
import com.inoka.inoka_app.model.ChatType;
import com.inoka.inoka_app.model.Game;
import com.inoka.inoka_app.model.Player;
import com.inoka.inoka_app.model.PlayerView;

@Service
public class ChatService {
    private static final int MAX_MESSAGES_PER_GAME = 32;

    private final Map<String, Deque<ChatMessage>> chatMap = new ConcurrentHashMap<>();
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;
    
    public ChatService(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Access concurrent chatMap and append incoming messages to Game chat history
     * @param gameId
     * @param message
     */
    private void append(String gameId, ChatMessage message) {
        
    }
    

    /**
     * Called by WebSocketController to process incoming user messages
     * <p> {@code Player sender} extracted from Principle in WebSocketController </p>
     * <p> Automatically calls methods to append to chatMap and broadcast </p>
     * @param gameId UUID of Game object
     * @param sender UUID of Player who sent message in chat
     * @param content Message player provided in chat
     */
    public void handlePlayerMessage(String gameId, String sender, String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        Optional<Game> gameOpt = gameService.getGameById(gameId);
        if (gameOpt.isEmpty()) {
            return;
        }
        Game game = gameOpt.get();
        Player player = game.getPlayer(sender);

        PlayerView senderView = PlayerView.fromPlayer(player, game.getSeatForPlayer(sender));
        ChatMessage message = new ChatMessage(senderView, trimmed, Instant.now(), ChatType.PLAYER);

        // Append to map and broadcast
    }
}
