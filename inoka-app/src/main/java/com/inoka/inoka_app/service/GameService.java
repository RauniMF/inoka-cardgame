package com.inoka.inoka_app.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.inoka.inoka_app.model.Player;
import com.inoka.inoka_app.model.Card;
import com.inoka.inoka_app.model.CardStyle;
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
        // Mutable object you can modify inside lambda expression
        final List<Optional<List<Player>>> result = new ArrayList<>(1);
        result.add(Optional.empty());
        games.computeIfPresent(gameId, (id, game) -> {
            synchronized (game) {
                result.set(0, Optional.of(new ArrayList<>(game.getPlayers().values())));
                return game;
            }
        });

        return result.get(0);
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

    /*
     * Given the UUID of a game,
     * set the GameState to DRAWING_CARDS
     * and queue broadcast
     */
    public void setGameStart(String gameId) {
        games.computeIfPresent(gameId, (id, game) -> {
            synchronized (game) {
                game.setState(GameState.DRAWING_CARDS);
                queueGameUpdate(gameId);
                return game;
            }
        });
    }

    public boolean setClashStart(String gameId) {
        final List<Boolean> result = new ArrayList<>(1);
        result.add(false);
        games.computeIfPresent(gameId, (id, game) -> {
            synchronized (game) {
                if (game.getState() == GameState.COUNT_DOWN ||
                    game.getState() == GameState.CLASH_CONCLUDED) {
                        game.setState(GameState.CLASH_ROLL_INIT);
                        // Initiative values are re-rolled at start of clash
                        game.resetInitiativeValue();
                        result.set(0, true);
                        queueGameUpdate(gameId);
                    }
                return game;
            }
        });
        return result.get(0);
    }

    public boolean setClashFinishedProcessing(String gameId) {
        final List<Boolean> result = new ArrayList<>(1);
        result.add(false);
        games.computeIfPresent(gameId, (id, game) -> {
            synchronized (game) {
                if (game.getState() == GameState.CLASH_PROCESSING_DECISION) {
                    game.setState(GameState.CLASH_PLAYER_TURN);
                    // Move onto next player's turn
                    game.determineNextInitiativeValue();
                    result.set(0, true);
                    queueGameUpdate(gameId);
                }
                return game;
            }
        });
        return result.get(0);
    }

    /*
     * Given the UUID of a player and a Card object,
     * Add the card to the Map of cards in play
     * in the game the player is in
     * Returns true if successful, false otherwise
     */
    public boolean putCardInPlay(String playerId, Card card) {
        Optional<Player> playerCheck = gameRepo.findPlayerById(playerId);
        if (playerCheck.isPresent()) {
            Player player = playerCheck.get();

            String gameId = player.getGameId();
            games.computeIfPresent(gameId, (id, game) -> {
                synchronized (game) {
                    Player playerTransient = game.getPlayer(player.getId());
                    game.addCardInPlay(playerId, card);
                    playerTransient.removeCardFromDeck(card);
                    /*
                     * If all players have put a card in play,
                     * and game is currently in the DRAWING_CARDS state,
                     * Set state to COUNT_DOWN
                     */
                    if (game.getState() == GameState.DRAWING_CARDS) {
                        if (game.getCardsInPlay().size() == game.getPlayers().size()) {
                            game.setState(GameState.COUNT_DOWN);
                        }
                    }
                    queueGameUpdate(gameId);
                    return game;
                }
            });
            return true;
        } else {
            return false;
        }
    }

    public int rollInitForPlayer(String playerId) {
        Optional<Player> playerCheck = gameRepo.findPlayerById(playerId);
        if (playerCheck.isPresent()) {
            Player player = playerCheck.get();

            String gameId = player.getGameId();
            games.computeIfPresent(gameId, (id, game) -> {
                synchronized (game) {
                    // Players cannot share an existing initiative value
                    do {
                        Player playerTransient = game.getPlayer(player.getId());
                        playerTransient.rollInitiative();
                        Card playerCardInPlay = game.getPlayerCardInPlay(playerId);
                        // Tricksters add their level to initiative when put in play
                        if (playerCardInPlay != null && playerCardInPlay.getStyle() == CardStyle.TRICKSTER) {
                            playerTransient.addToInitiative(playerCardInPlay.getLevel());
                        }
                        player.setInitiative(playerTransient.getInitiative());
                    } while (!game.addPlayerInitiativeToMap(player));
                    /*
                     * If all players have rolled initiative,
                     * and game is currently in CLASH_ROLL_INIT state,
                     * set state to CLASH_PLAYER_TURN
                     */
                    if (game.getState() == GameState.CLASH_ROLL_INIT) {
                        if (game.getInitiativeMap().size() == game.getPlayers().size()) {
                            game.setState(GameState.CLASH_PLAYER_TURN);
                            game.determineNextInitiativeValue();
                        }
                    }
                    queueGameUpdate(gameId);
                    return game;
                }
            });
            return player.getInitiative();
        }
        return -1;
    }

    /*
     * Given the UUID of a player taking action on a CLASH_PLAYER_TURN,
     * and the UUID of the player whose card was chosen to take damage,
     * or "null" if a player chose to skip their turn,
     * deal the damage and change GameState to CLASH_PROCESSING_DECISION,
     * returning how much damage was dealt,
     * or -1 if the player chose to skip their turn
     */
    public int resolveClashAction(String dealingPlayerId, String receivingPlayerId) {
        Optional<Player> playerCheck = gameRepo.findPlayerById(dealingPlayerId);
        final List<Integer> result = new ArrayList<>(1);
        result.add(-1);
        if (playerCheck.isPresent()) {
            Player player = playerCheck.get();

            String gameId = player.getGameId();
            games.computeIfPresent(gameId, (id, game) -> {
                synchronized (game) {
                    if (!receivingPlayerId.equals("null")) {
                        int damage = game.dealDamage(dealingPlayerId, receivingPlayerId);
                        result.set(0, damage);
                    }
                    game.setLastAction(dealingPlayerId, receivingPlayerId, result.get(0));
                    game.setState(GameState.CLASH_PROCESSING_DECISION);
                    queueGameUpdate(gameId);
                    return game;
                }
            });
        }
        return result.get(0);
    }
}
