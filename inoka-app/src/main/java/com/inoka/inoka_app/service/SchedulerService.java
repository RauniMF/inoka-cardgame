package com.inoka.inoka_app.service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.inoka.inoka_app.model.Game;

@Service
public class SchedulerService {

    private final GameService gameService;

    private final SimpMessagingTemplate messagingTemplate;
    private final CopyOnWriteArraySet<String> pendingGameUpdates = new CopyOnWriteArraySet<>();
    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    public SchedulerService(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
        scheduler.initialize();
        startBatchUpdateTask();
    }

    // Broadcasts pending changes to Game objects on a fixed 500ms interval
    private void startBatchUpdateTask() {
        scheduler.scheduleAtFixedRate(this::broadcastPendingUpdates, Duration.ofMillis(500));
    }

    private void broadcastPendingUpdates() {
        for (String gameId: pendingGameUpdates) {
            Optional<Game> gameOpt = gameService.getGameById(gameId);
            gameOpt.ifPresent(game -> {
                messagingTemplate.convertAndSend("/topic/game/" + game.getId(), game);
            });
        }
        pendingGameUpdates.clear();
    }

    public void queueGameUpdate(String gameId) {
        pendingGameUpdates.add(gameId);
    }
}
