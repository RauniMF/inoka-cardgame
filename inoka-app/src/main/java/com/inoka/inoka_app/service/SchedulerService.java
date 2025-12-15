package com.inoka.inoka_app.service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.inoka.inoka_app.event.GameUpdateEvent;
import com.inoka.inoka_app.model.Game;

@Service
public class SchedulerService {

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
        for (Game game : pendingGameUpdates.values()) {
            messagingTemplate.convertAndSend("/topic/game/" + game.getId(), game);
        }
        pendingGameUpdates.clear();
    }

    public void queueGameUpdate(Game game) {
        pendingGameUpdates.put(game.getId(), game);
    }

    /**
     * Event listener that handles game update events.
     * This completely decouples SchedulerService from GameService,
     * eliminating the circular dependency.
     */
    @EventListener
    public void handleGameUpdateEvent(GameUpdateEvent event) {
        queueGameUpdate(event.getGame());
    }
}
