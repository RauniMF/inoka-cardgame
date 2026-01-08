package com.inoka.inoka_app.service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.inoka.inoka_app.event.GameUpdateEvent;
import com.inoka.inoka_app.model.Game;
import com.inoka.inoka_app.model.Player;
import com.inoka.inoka_app.model.GameView;

@Service
public class SchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<String, Game> pendingGameUpdates = new ConcurrentHashMap<>();
    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    public SchedulerService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        scheduler.initialize();
        startBatchUpdateTask();
    }

    // Broadcasts pending changes to Game objects on a fixed 500ms interval
    private void startBatchUpdateTask() {
        scheduler.scheduleAtFixedRate(this::broadcastPendingUpdates, Duration.ofMillis(500));
    }

    private void broadcastPendingUpdates() {
        if (!pendingGameUpdates.isEmpty()) {
            logger.debug("Broadcasting {} pending game update(s)", pendingGameUpdates.size());
            
            for (Game game : pendingGameUpdates.values()) {
                // Broadcast sanitized GameView to all players in the game
                String gameDestination = "/topic/game/" + game.getId();
                GameView gameView = GameView.fromGame(game);
                messagingTemplate.convertAndSend(gameDestination, gameView);
                logger.debug("Sent GameView for game {} to {}", game.getId(), gameDestination);
                
                // Send each player their private deck data
                for (Map.Entry<String, Player> entry : game.getPlayers().entrySet()) {
                    String playerId = entry.getKey();
                    Player player = entry.getValue();
                    
                    // Send to user-specific queue: /user/{playerId}/queue/deck
                    messagingTemplate.convertAndSendToUser(
                        playerId,
                        "/queue/deck",
                        player.getDeck()
                    );
                    logger.debug("Sent deck update to player {} in game {}", playerId, game.getId());
                }
            }
            
            pendingGameUpdates.clear();
        }
    }

    public void queueGameUpdate(Game game) {
        pendingGameUpdates.put(game.getId(), game);
    }

    @EventListener
    public void handleGameUpdateEvent(GameUpdateEvent event) {
        queueGameUpdate(event.getGame());
    }
}
