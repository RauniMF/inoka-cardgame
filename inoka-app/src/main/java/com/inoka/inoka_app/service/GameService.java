package com.inoka.inoka_app.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.inoka.inoka_app.model.Player;
import com.inoka.inoka_app.model.Card;
import com.inoka.inoka_app.model.Game;
import com.inoka.inoka_app.model.GameState;
import com.inoka.inoka_app.repositories.PlayerRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.List;

@Service
public class GameService {
    // Repo containing player data (name, id, gameid)
    private final PlayerRepository gameRepo;
    // Transient game data stored in HashMap
    private final ConcurrentHashMap<String, Game> games = new ConcurrentHashMap<>();;

    // Handle broadcasting changes made to Game objects
    private final SimpMessagingTemplate messagingTemplate;
    private final CopyOnWriteArraySet<String> pendingGameUpdates = new CopyOnWriteArraySet<>();
    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    public GameService(PlayerRepository gameRepo, SimpMessagingTemplate messagingTemplate) {
        this.gameRepo = gameRepo;
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
            games.computeIfPresent(gameId, (String id, Game game) -> {
                synchronized (game) {
                    messagingTemplate.convertAndSend("/topic/game/"+ game.getId(), game);
                }
                return game;
            });
        }
        pendingGameUpdates.clear();
    }

    public void queueGameUpdate(String gameId) {
        pendingGameUpdates.add(gameId);
    }

    public Player addPlayer(Player player) {
        return gameRepo.save(player);
    }
    public List<Player> findAllPlayers() {
        return gameRepo.findAll();
    }
    public boolean updatePlayer(String id, String name) {
        Optional<Player> playerCheck = gameRepo.findPlayerById(id);
        if (playerCheck.isPresent()) {
            Player player = playerCheck.get();
            player.setName(name);
            gameRepo.save(player);
            return true;
        } else {
            return false;
        }
    }
    public boolean removePlayerById(String id) {
        if (gameRepo.existsById(id)) {
            gameRepo.deleteById(id);
            return true;
        }
        else {
            return false;
        }
    }
    public Player findPlayerById(String id) {
        Optional<Player> playerCheck = gameRepo.findPlayerById(id);
        if (playerCheck.isPresent()) {
            return playerCheck.get();
        }
        return null;
    }
    public void removeAllPlayers() {
        gameRepo.deleteAll();
    }
    public Optional<List<Card>> getPlayerDeck(String playerId) {
        Optional<Player> playerCheck = gameRepo.findPlayerById(playerId);
        if (!playerCheck.isPresent()) {
            return Optional.empty();
        }
        Player playerTransient = playerCheck.get();
        String gameId = playerTransient.getGameId();
        if ((gameId.equals("Not in game")) || (gameId == null) || (gameId.isEmpty())) {
            return Optional.empty();
        }
        // Now we know the player is in a game, so we find the game in the game map
        Game game = games.get(gameId);
        // Now we access the player's deck through the game, which is holding its value in memory
        Player player = game.getPlayers().get(playerId);
        if (player != null) {
            return Optional.of(player.getDeck());
        }
        return Optional.empty();
    }
    
    /*
     * Creates or joins existing game
     */
    public synchronized Game createGame(String passcode, Player player) {
        if (passcode == null || passcode.isEmpty()) {
            // Check if there's an existing game without a passcode and in WAITING_FOR_PLAYERS state
            for (Game game : games.values()) {
                if ((game.getPasscode() == null || game.getPasscode().isEmpty()) && game.getState() == GameState.WAITING_FOR_PLAYERS) {
                    // Add the player to the existing game
                    this.addPlayerToGame(game.getId(), player);
                    return game;
                }
            }
        }
        // Check if there's a game with the same passcode and in WAITING_FOR_PLAYERS state
        for (Game game : games.values()) {
            if (game.getPasscode() != null && game.getPasscode().equals(passcode)) {
                if (game.getState() == GameState.WAITING_FOR_PLAYERS) {
                    this.addPlayerToGame(game.getId(), player);
                    return game;
                }
            }
        }

        // If no suitable game was found, create a new game with the given passcode
        Game game = (passcode != null && !passcode.isEmpty()) ? new Game(passcode) : new Game();
        games.put(game.getId(), game);
        this.addPlayerToGame(game.getId(), player);
        return game;
    }

    public Game getGame(String id) {
        return games.get(id);
    }
    public void addGame(Game game) {
        games.put(game.getId(), game);
    }
    public void removeGame(String id) {
        games.remove(id);
    }
    public Map<String,Game> getAllGames() {
        return games;
    }

    /*
     * Given a passcode,
     * Add player to open lobby with passcode
     * Returns true if player was successfully added
     * Else, false
     */
    public void addPlayerToGame(String gameId, Player player) {
        games.computeIfPresent(gameId, (id, game) -> {
            synchronized (game) {
                game.addPlayer(player);
                gameRepo.save(player);
                queueGameUpdate(gameId);
                return game;
            }
        });
    }

    /*
     * Given the UUID of a game,
     * Return a List of players in game
     */
    public Optional<List<Player>> getPlayersInGame(String gameId) {
        Game game = this.getGame(gameId);

        if (game == null) {
            return Optional.empty();
        }

        if (game.getPlayers().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ArrayList<>(game.getPlayers().values()));
    }

    /*
     * Given a player's UUID,
     * set the player's isReady to True
     * Returns true if success
     * else false
     */
    public boolean setPlayerReady(String playerId) {
        Optional<Player> playerCheck = gameRepo.findPlayerById(playerId);
        if (playerCheck.isPresent()) {
            Player player = playerCheck.get();
            // Set player ready in transient Game data
            String gameId = player.getGameId();
            
            games.computeIfPresent(gameId, (id, game) -> {
                synchronized (game) {
                    Player playerTransient = game.getPlayer(player.getId());
                    playerTransient.setReady(true);
                    queueGameUpdate(gameId);
                    return game;
                }
            });
            return true;
        } else {
            return false;
        }
    }

    /*
     * Given the UUID of a game,
     * Check if all players in a game are ready to start match
     * Returns Optional.empty() if gameId is invalid
     * Returns true if all players in game are ready
     * Returns false otherwise
     */
    public Optional<Boolean> allPlayersReady(String gameId) {
        Optional<List<Player>> playersCheck = this.getPlayersInGame(gameId);

        if (playersCheck.isEmpty()) {
            return Optional.empty();
        }

        for (Player player : playersCheck.get()) if(!player.isReady()) return Optional.of(false);

        return Optional.of(true);
    }
}
