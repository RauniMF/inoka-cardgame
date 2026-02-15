package com.inoka.inoka_app.service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.inoka.inoka_app.event.SystemMessageEvent;
import com.inoka.inoka_app.model.ChatMessage;
import com.inoka.inoka_app.model.ChatType;
import com.inoka.inoka_app.model.Game;
import com.inoka.inoka_app.model.Player;
import com.inoka.inoka_app.model.PlayerView;

@Service
public class ChatService {
    private static final int MAX_MESSAGES_PER_GAME = 32;

    private final ConcurrentHashMap<String, Deque<ChatMessage>> chatMap = new ConcurrentHashMap<>();
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
        chatMap.compute(gameId, (id, deque) -> {
            if (deque == null) {
                deque = new ArrayDeque<>();
            }
            synchronized(deque) {
                deque.addLast(message);
                while (deque.size() > MAX_MESSAGES_PER_GAME) {
                    deque.removeFirst();
                }
                return deque;
            }
        });
    }
    
    public void clear(String gameId) {
        chatMap.remove(gameId);
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

        append(gameId, message);
        broadcast(gameId, message);
    }

    public List<ChatMessage> getHistory(String gameId) {
        // Stored messages include PLAYER and INFO
        Deque<ChatMessage> deque = chatMap.get(gameId);
        if (deque == null) {
            return List.of();
        }
        List<ChatMessage> messages = new ArrayList<>(deque);
        return messages;
    }

    public void addSystemMessage(String gameId, String content, ChatType type) {
        ChatMessage message = new ChatMessage(null, content, Instant.now(), type);
        // Stored messages include PLAYER and INFO
        if (type != ChatType.WARNING) {
            append(gameId, message);
        }
        broadcast(gameId, message);
    }

    public void broadcast(String gameId, ChatMessage message) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/chat", message);
    }

    @EventListener
    public void onSystemMessageEvent(SystemMessageEvent event) {
        if (event.getContent().equals("GAME CONCLUDED")) {
            // Game concluded = remove chat history
            clear(event.getGameId());
            return;
        }
        addSystemMessage(event.getGameId(), event.getContent(), event.getType());
    }
}
