package com.inoka.inoka_app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.inoka.inoka_app.event.DeckUpdateEvent;
import com.inoka.inoka_app.event.GameUpdateEvent;
import com.inoka.inoka_app.model.Player;
import jakarta.annotation.PostConstruct;

import com.inoka.inoka_app.model.Action;
import com.inoka.inoka_app.model.Card;
import com.inoka.inoka_app.model.CardStyle;
import com.inoka.inoka_app.model.Game;
import com.inoka.inoka_app.model.GameState;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

@Service
public class GameService {
    // Repo containing player data (name, id, gameid)
    private final PlayerService playerService;

    private final ApplicationEventPublisher eventPublisher;
    // Transient game data stored in HashMap
    private final ConcurrentHashMap<String, Game> games = new ConcurrentHashMap<>();;

    @Value("${game.inactivity.timeout-ms}")
    private long inactivityTimeoutMs;

    @Value("${game.cleanup.timeout-ms}")
    private long cleanupTimeoutMs;

    @Value("${game.clash.processing-delay}")
    private long classProcessDelayMs;

    @Value("${game.scheduler.check-interval-ms}")
    private long schedulerCheckIntervalMs;

    @Value("${game.all-players-ready-delay}")
    private long allPlayersReadyDelayMs;

    @Value("${game.inactivity-kick.timeout-ms}")
    private long kickInactivityTimeoutMs;

    public GameService(PlayerService playerService, ApplicationEventPublisher eventPublisher) {
        this.playerService = playerService;
        this.eventPublisher = eventPublisher;
    }
    
    // TODO: Revisit concurrent implementation
    private void publishGameUpdate(String gameId) {
        Game game = games.get(gameId);
        if (game != null) {
            eventPublisher.publishEvent(new GameUpdateEvent(this, game));
        }
    }
    private void publishDeckUpdate(Player player) {
        List<Card> deck = player.getDeck();
        eventPublisher.publishEvent(new DeckUpdateEvent(this, player.getId(), deck));
    }

    @PostConstruct
    private void startClashDecisionAutoAdvanceTask() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("clash-decision-advance-");
        scheduler.initialize();
        scheduler.scheduleAtFixedRate(
            this::checkAndAdvanceClashDecisions,
            Duration.ofMillis(schedulerCheckIntervalMs)
        );
    }
    private void checkAndAdvanceClashDecisions() {
        long now = System.currentTimeMillis();
        for (Game game : games.values()) {
            synchronized (game) {
                // Advance clash processing state timer
                if (game.getState() == GameState.CLASH_PROCESSING_DECISION) {
                    if (now - game.getClashProcessingTimestamp() >= classProcessDelayMs) {
                        this.processClashDecision(game.getId());
                    }
                }
                // Advance lobby players all ready timer
                if (game.getState() == GameState.ALL_PLAYERS_READY) {
                    if (now - game.getClashProcessingTimestamp() >= allPlayersReadyDelayMs) {
                        this.setGameStart(game.getId());
                    }
                }
                // Inactive players timers - Lobby
                if (game.getState() == GameState.WAITING_FOR_PLAYERS) {
                    for (Player player : game.getPlayers().values()) {
                        if (!player.isReady()) {
                            Long joinTime = game.getPlayerLastActivityTimestamp(player.getId());
                            if (joinTime != null && (now - joinTime) >= inactivityTimeoutMs) {
                                this.removePlayerFromGame(player.getId());
                            }
                        }
                    }
                }
                // Inactive players timer - In Game
                if (
                    game.getState() == GameState.DRAWING_CARDS ||
                    game.getState() == GameState.CLASH_PLAYER_TURN ||
                    game.getState() == GameState.CLASH_PLAYER_REPLACING_CARD
                ) {
                    if (now - game.getLastActivityTimestamp() >= inactivityTimeoutMs) {
                        this.resolveInactivePlayerAction(game.getId());
                    }
                }
                // Finished game cleanup timer
                if (game.getState() == GameState.FINISHED) {
                    if (now - game.getLastActivityTimestamp() >= cleanupTimeoutMs) {
                        this.removeFinishedGame(game.getId());
                    }
                }
            }
            
        }
    }
    

    // =====================================================
    // CRUD Methods
    // =====================================================

    public void addGame(Game game) {
        games.put(game.getId(), game);
    }
    
    public void removeGame(String id) {
        games.remove(id);
    }
    
    public List<Game> getAllGames() {
        return new ArrayList<>(games.values());
    }

    public Optional<Game> getGameById(String gameId) {
        return Optional.ofNullable(this.games.get(gameId));
    }

    public boolean gameWithIdExists(String gameId) {
        return this.games.contains(gameId);
    }

    public Optional<Game> getGameByPlayerId(String playerId) {
        Optional<Player> playerOpt = playerService.findPlayerById(playerId);
        if (playerOpt.isEmpty()) return Optional.empty();

        String gameId = playerOpt.get().getGameId();
        return this.getGameById(gameId);
    }

    /**
     * Returns the transient deck (List of Card objects) belonging to player with input playerId
     * @param playerId Player UUID
     * @return {@code Optional<List<Card>>} representing present deck data belonging to the player 
     * stored in transient Game object, or {@code Optional.empty} if not found
     */
    public Optional<List<Card>> getPlayerDeck(String playerId) {
        Optional<Player> playerOpt = playerService.findPlayerById(playerId);
        if (!playerOpt.isPresent()) {
            return Optional.empty();
        }
        Player playerTransient = playerOpt.get();
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
    
    /**
     * Creates or joins existing game
     * @param passcode Used to identify game lobby to join
     * @param player Player object to add to game
     */
    public synchronized Game createGame(String passcode, Player player) {
        // Check if player can join existing game
        // Can also use .orElseGet() for more succinct code but I think this is a little more readable
        Optional<Game> gameOpt = this.joinGame(passcode, player);
        if (gameOpt.isPresent()) return gameOpt.get();

        // If no suitable game was found, create a new game with the given passcode
        Game game = (passcode != null && !passcode.isEmpty()) ? new Game(passcode) : new Game();
        games.put(game.getId(), game);
        this.addPlayerToGame(game.getId(), player);
        return game;
    }
    
    /**
     * Joins game if game exists & {@code game.numPlayers() < 6}
     */
    public synchronized Optional<Game> joinGame(String passcode, Player player) {
        // Checks if player can join existing game
        for (Game game : games.values()) {
            if (passcode == null || passcode.isEmpty()) {
                // No passcode
                if ((game.getPasscode() == null || game.getPasscode().isEmpty()) &&
                    (game.getState() == GameState.WAITING_FOR_PLAYERS) &&
                    (game.numPlayers() < 6)) {
                    // Add the player to the existing game
                    this.addPlayerToGame(game.getId(), player);
                    return Optional.of(game);
                }
            }
            else {
                // passcode
                if ((game.getPasscode() != null && game.getPasscode().equals(passcode)) &&
                    (game.getState() == GameState.WAITING_FOR_PLAYERS) &&
                    (game.numPlayers() < 6)) {
                    this.addPlayerToGame(game.getId(), player);
                    return Optional.of(game);
                }
            }
        }
        return Optional.empty();
    }

    public void addPlayerToGame(String gameId, Player player) {
        games.computeIfPresent(gameId, (id, game) -> {
            synchronized (game) {
                game.addPlayer(player);
                game.updatePlayerLastActivityTimestamp(player.getId());
                this.playerService.savePlayer(player);
                this.publishGameUpdate(gameId);
                return game;
            }
        });
    }

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

    /**
     * Removes a Game in GameState.FINISHED from memory
     * and resets Player game ID
     * @param gameId Game UUID to remove
     */
    public void removeFinishedGame(String gameId) {
        Optional<Game> gameOpt = this.getGameById(gameId);
        if (
            gameOpt.isPresent() &&
            gameOpt.get().getState() == GameState.FINISHED
        ) {
            Game game = games.remove(gameId);
            for (Player player : game.getPlayers().values()) {
                if (player.getGameId() == game.getId()) {
                    player.setGameId("Not in game");
                    this.playerService.savePlayer(player);
                }
            }
        }
    }


    // =====================================================
    // Player Action Methods
    // =====================================================

    public void updatePlayerActivity(String playerId) {
        Optional<Player> playerOpt = this.playerService.findPlayerById(playerId);
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();
            String gameId = player.getGameId();

            games.computeIfPresent(gameId, (id, game) -> {
                synchronized (game) {
                    game.updatePlayerLastActivityTimestamp(playerId);
                    return game;
                }
            });
        }
    }

    /**
     * Given a player's UUID, set the corresponding Player object's {@code isReady} attribute
     * stored in the transient Game data to True
     * @param playerId Player UUID
     * @return {@code boolean} True if Player.isReady set to True, False otherwise
     */
    public boolean setPlayerReady(String playerId) {
        Optional<Player> playerOpt = this.playerService.findPlayerById(playerId);
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();
            // Set player ready in transient Game data
            String gameId = player.getGameId();
            
            games.computeIfPresent(gameId, (id, game) -> {
                synchronized (game) {
                    Player playerTransient = game.getPlayer(player.getId());
                    playerTransient.setReady(true);
                    
                    // Check if all players are ready
                    boolean allReady = this.allPlayersReady(gameId).get();
                    if (allReady && game.numPlayers() >= 2) {
                        game.setState(GameState.ALL_PLAYERS_READY);
                        game.setProcessingTimestamp(System.currentTimeMillis());
                    }
                    
                    this.publishGameUpdate(gameId);
                    return game;
                }
            });
            return games.get(gameId).getPlayer(playerId).isReady();
        } else {
            return false;
        }
    }

    /**
     * Given the UUID of a game, check if all players in a game are ready to start match
     * TODO: Revisit check for allPlayersReady in frontend to improve application responsiveness
     * @param gameID Game UUID
     * @return {@code Optional<Boolean>} Optional.empty if gameId is invalid,
     * returns True if all players in game are ready, False otherwise
     */
    public Optional<Boolean> allPlayersReady(String gameId) {
        Optional<List<Player>> playersListOpt = this.getPlayersInGame(gameId);

        if (playersListOpt.isEmpty()) {
            return Optional.empty();
        }

        for (Player player : playersListOpt.get()) if(!player.isReady()) return Optional.of(false);

        return Optional.of(true);
    }

    /**
     * Given the UUID of a player and a Card object,
     * add the card to the Map of cards in play.
     * Also handles GameState changes from {@code DRAWING_CARDS} to {@code COUNT_DOWN},
     * as well as {@code CLASH_PLAYER_REPLACING_CARD} to {@code CLASH_PLAYER_TURN}
     * @param playerId Player UUID
     * @param card Card object to put in play
     * @return {@code boolean} True if input playerId points to existing Player, False otherwise
     */
    public boolean putCardInPlay(String playerId, Card card) {
        Optional<Player> playerOpt = this.playerService.findPlayerById(playerId);
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();

            String gameId = player.getGameId();
            final List<Boolean> result = new ArrayList<>(1);
            result.add(false);
            
            games.computeIfPresent(gameId, (id, game) -> {
                synchronized (game) {
                    if (
                        game.getState() != GameState.DRAWING_CARDS &&
                        game.getState() != GameState.CLASH_PLAYER_REPLACING_CARD
                    ) {
                        return game;
                    }
                    
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
                    /*
                     * If a player's previous card got defeated in clash,
                     * and they put another card in play,
                     * update GameState from CLASH_PLAYER_REPLACING_CARD
                     * to CLASH_PLAYER_TURN, updating initiative value
                     */
                    if (game.getState() == GameState.CLASH_PLAYER_REPLACING_CARD) {
                        game.determineNextInitiativeValue();
                        game.setState(GameState.CLASH_PLAYER_TURN);
                        // Start timer for next player to take action
                        game.setLastActivityTimestamp(System.currentTimeMillis());
                    }
                    this.publishGameUpdate(gameId);
                    this.publishDeckUpdate(player);
                    result.set(0, true);
                    return game;
                }
            });
            return result.get(0);
        } else {
            return false;
        }
    }

    /**
     * Rolls unique initiative value for player with input playerId in transient Game object
     * @param playerId Player UUID
     * @return {@code int} value rolled for initiative if successful, or -1 otherwise
     */
    public int rollInitForPlayer(String playerId) {
        Optional<Player> playerOpt = this.playerService.findPlayerById(playerId);
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();

            String gameId = player.getGameId();
            final List<Integer> result = new ArrayList<>(1);
            result.add(-1);
            
            games.computeIfPresent(gameId, (id, game) -> {
                synchronized (game) {
                    if (game.getState() != GameState.CLASH_ROLL_INIT) {
                        return game;
                    }
                    
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
                            // Start timer for next player to take action
                            game.setLastActivityTimestamp(System.currentTimeMillis());
                            game.determineNextInitiativeValue();
                        }
                    }
                    this.publishGameUpdate(gameId);
                    result.set(0, player.getInitiative());
                    return game;
                }
            });
            return result.get(0);
        }
        return -1;
    }

    /**
     * On {@code Game.state == CLASH_PLAYER_TURN}, given the input UUID of a player taking action
     * and the UUID of the player they chose to take action against, or "null" if that player
     * chose to skip their turn, deal damage and/or change GameState to {@code CLASH_PROCESSING_DECISION}
     * @param dealingPlayerId Dealing Player UUID
     * @param receivingPlayerId Receiving Player UUID
     * @return {@code int} damage dealt to receiving Player's Card, -1 if no damage dealt.
     */
    public int resolveClashAction(String dealingPlayerId, String receivingPlayerId) {
        Optional<Player> playerOpt = this.playerService.findPlayerById(dealingPlayerId);
        final List<Integer> result = new ArrayList<>(1);
        result.add(-1);
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();

            String gameId = player.getGameId();
            games.computeIfPresent(gameId, (id, game) -> {
                synchronized (game) {
                    // Guard: only allow actions during CLASH_PLAYER_TURN
                    if (game.getState() != GameState.CLASH_PLAYER_TURN) {
                        return game;
                    }

                    int damage = -1;
                    if (!receivingPlayerId.equals("null")) {
                        // Deal damage
                        damage = game.dealDamage(dealingPlayerId, receivingPlayerId);
                        result.set(0, damage);
                    } 
                    
                    game.setLastAction(dealingPlayerId, receivingPlayerId, result.get(0));
                    game.setState(GameState.CLASH_PROCESSING_DECISION);
                    game.setProcessingTimestamp(System.currentTimeMillis());
                    this.publishGameUpdate(gameId);
                    return game;
                }
            });
        }
        return result.get(0);
    }

    /**
     * @deprecated Remove after confirming forfeit / card removal
     * work during CLASH_PLAYER_TURN
     * @param playerId
     * @return
     */
    @Deprecated
    public boolean removePlayerCardInPlay(String playerId) {
        Optional<Player> playerOpt = this.playerService.findPlayerById(playerId);
        final List<Boolean> result = new ArrayList<>(1);
        result.add(false);
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();

            String gameId = player.getGameId();
            games.computeIfPresent(gameId, (id, game) -> {
                synchronized (game) {
                    // If card not present, no-op
                    if (!game.getCardsInPlay().containsKey(playerId)) {
                        return game;
                    }
                    
                    Card removedCard = game.removeCardInPlay(playerId);
                    if (removedCard != null) {
                        result.set(0, true);
                        // Note: State transitions now handled in resolveClashAction
                        // This method is kept for legacy/explicit removal if needed
                        if (game.getState() == GameState.CLASH_PROCESSING_DECISION) {
                            game.setState(GameState.CLASH_PLAYER_REPLACING_CARD);
                        }
                        this.publishGameUpdate(gameId);
                    }
                    return game;
                }
            });
        }
        return result.get(0);
    }

    /**
     * Given a player's UUID,
     * discard their card in play
     * and remove them from the initiative order
     */
    public void playerForfeitClash(String playerId) {
        Optional<Player> playerOpt = this.playerService.findPlayerById(playerId);
        
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();

            String gameId = player.getGameId();
            games.computeIfPresent(gameId, (id, game) -> {
                synchronized (game) {
                    if (
                        !game.getCardsInPlay().containsKey(playerId) &&
                        !((game.getState() == GameState.CLASH_PLAYER_REPLACING_CARD) ||
                        (game.getState() == GameState.CLASH_PLAYER_TURN))
                    ) {
                        return game;
                    }
                    // Remove card in play
                    game.removeCardInPlay(playerId);
                    // Handle initiative order
                    // Forfeit during turn: remove from order then update lastAction
                    game.removePlayerFromInitiative(player);
                    // Update game state & last action
                    game.setLastAction("null", playerId, -1);
                    game.setState(GameState.CLASH_PROCESSING_DECISION);
                    game.setProcessingTimestamp(System.currentTimeMillis());
                    this.publishGameUpdate(gameId);
                    return game;
                }
            });
        }
    }

    /**
     * Resolves inactive player to default behavior.
     * <p>Behavior depends on GameState:
     * - {@code DRAWING_CARDS}: Select first card from player's hand to put in play
     * - {@code CLASH_PLAYER_TURN}: Skip player's turn
     * - {@code CLASH_PLAYER_REPLACING_CARD}: Player forfeits clash
     * </p>
     * @param gameId Game UUID
     */
    void resolveInactivePlayerAction(String gameId) {
        games.computeIfPresent(gameId, (id, game) -> {
            synchronized (game) {
                if (game.getState() == GameState.DRAWING_CARDS) {
                    for (Player player : game.getPlayers().values()) {
                        if (
                            !game.getCardsInPlay().containsKey(player.getId()) &&
                            player.getDeckSize() > 0
                        ) {
                            Card firstCard = player.getDeck().get(0);
                            this.putCardInPlay(player.getId(), firstCard);
                        }
                    }
                }
                else if (game.getState() == GameState.CLASH_PLAYER_TURN) {
                    String currentPlayerId = game.getInitiativeMap().get(game.getCurrentInitiativeValue());
                    if (currentPlayerId != null) {
                        this.resolveClashAction(currentPlayerId, "null");
                    }
                }
                else if (game.getState() == GameState.CLASH_PLAYER_REPLACING_CARD) {
                    for (String playerId : game.getInitiativeMap().values()) {
                        if (!game.getCardsInPlay().containsKey(playerId)) {
                            this.playerForfeitClash(playerId);
                            break;
                        }
                    }
                }
                return game;
            }
        });
    }
    
    /**
     * Removes a player from a game and handles cleanup.
     * <p> Handles:
     * - Removal from all game data structures (players, initiative, cardsInPlay, lastActivity)
     * - Resetting player gameId
     * - Checking if game should end (1 player remaining)
     * - Checking if game should be removed (0 players remaining)
     * - Recalculating readiness if in lobby states
     * </p>
     * @param playerId Player UUID to remove
     * @return
     */
    public boolean removePlayerFromGame(String playerId) {
        final List<Boolean> result = new ArrayList<>(1);
        result.add(false);

        Optional<Player> playerOpt = this.playerService.findPlayerById(playerId);
        
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();

            String gameId = player.getGameId();
            games.computeIfPresent(gameId, (id, game) -> {
                synchronized (game) {
                    if (!game.getPlayers().containsKey(playerId)) {
                        return game;
                    }

                    Optional<Player> updatedPlayerOpt = game.removePlayer(playerId);
                    if (updatedPlayerOpt.isEmpty()) {
                        return game;
                    }

                    this.playerService.savePlayer(updatedPlayerOpt.get());

                    int remainingPlayers = game.numPlayers();

                    if (remainingPlayers == 0) {
                        // No players left, mark for game removal
                        result.set(0, true);
                        return null; // remove game from map
                    }
                    else if (remainingPlayers == 1) {
                        // Only 1 player left, game ends
                        game.setState(GameState.FINISHED);
                        game.setLastActivityTimestamp(System.currentTimeMillis());
                        this.publishGameUpdate(gameId);
                        result.set(0, true);
                        return game;
                    }
                    else {
                        // 2+ players remaining, handle based on state
                        if (
                            game.getState() == GameState.WAITING_FOR_PLAYERS ||
                            game.getState() == GameState.ALL_PLAYERS_READY
                        ) {
                            boolean allReady = this.allPlayersReady(gameId).get();
                            if (allReady && remainingPlayers >= 2) {
                                game.setState(GameState.ALL_PLAYERS_READY);
                                game.setLastActivityTimestamp(System.currentTimeMillis());
                            }
                            else {
                                game.setState(GameState.WAITING_FOR_PLAYERS);
                            }
                        }

                        this.publishGameUpdate(gameId);
                        result.set(0, true);
                        return game;
                    }
                }
            });
        }

        return result.get(0);
    }


    // =====================================================
    // State Management Methods
    // =====================================================

    /**
     * Given the UUID of a game, set the GameState to DRAWING_CARDS if conditions are met
     * @param gameId Game UUID
     * @return {@code boolean} True if Game object was set to DRAWING_CARDS, False otherwise
     */
    public boolean setGameStart(String gameId) {
        final List<Boolean> result = new ArrayList<>(1);
        result.add(false);
        games.computeIfPresent(gameId, (id, game) -> {
            synchronized (game) {
                int numPlayers = game.numPlayers();
                if (game.getState() == GameState.ALL_PLAYERS_READY && numPlayers >= 2) {
                    game.setState(GameState.DRAWING_CARDS);
                    // Start timer for players to put a card in play
                    game.setLastActivityTimestamp(System.currentTimeMillis());
                    // All player activity refreshed
                    for (String playerId : game.getPlayers().keySet()) {
                        game.updatePlayerLastActivityTimestamp(playerId);
                    }
                    result.set(0, true);
                    this.publishGameUpdate(gameId);
                }
                else if (numPlayers < 2) {
                    game.setState(GameState.WAITING_FOR_PLAYERS);
                    this.publishGameUpdate(gameId);
                }
                return game;
            }
        });
        return result.get(0);
    }

    /**
     * Called by the frontend to process to CLASH_ROLL_INIT
     * from COUNT_DOWN
     * @param gameId UUID of Game
     * @return {@code boolean} if GameState is successfully changed to CLASH_ROLL_INIT
     */
    public boolean setClashStart(String gameId) {
        final List<Boolean> result = new ArrayList<>(1);
        result.add(false);
        games.computeIfPresent(gameId, (id, game) -> {
            synchronized (game) {
                if (game.getState() == GameState.COUNT_DOWN) {
                        game.setState(GameState.CLASH_ROLL_INIT);
                        // Initiative values are re-rolled at start of clash
                        game.resetInitiativeValue();
                        game.clearInitiativeMap();
                        result.set(0, true);
                        this.publishGameUpdate(gameId);
                    }
                return game;
            }
        });
        return result.get(0);
    }

    /**
     * Handles transitioning GameState for a Game after CLASH_CONCLUDED.
     * This includes checking for alternative win conditions.
     * 
     * <p>Win Condition Priority:
     * 1. Three Sacred Stones: Handled by {@code processClashDecision()}
     * 2. Last Player Standing: If only 1 player has any cards in their hand at the start
     * of a clash, they win (regardless of sacred stone count)
     * 3. Most Sacred Stones: If no players have cards remaining at the start of a clash,
     * the player with the most stones wins (with no tie breakers)
     * </p>
     * 
     * <p> If no win condition is met, the default behavior of proceeding to
     * DRAWING_CARDS is done, clearing cards from play prior to starting a new clash.
     * </p>
     * 
     * <p>Proper determination of player finishing placement and more in-depth
     * win condition logic is handled by PodiumView DTO after the game reaches FINISHED.
     * </p>
     * @param gameId Game UUID
     * @return {@code boolean} True if state successfully transitioned, False otherwise
     */
    public boolean startNewClash(String gameId) {
        final List<Boolean> result = new ArrayList<>(1);
        result.add(false);
        games.computeIfPresent(gameId, (id, game) -> {
            synchronized (game) {
                if (game.getState() == GameState.CLASH_CONCLUDED) {
                        // Remove cards from play
                        game.removeAllCardsFromPlay();
                    
                        // Check for win conditions
                        List<Player> players = new ArrayList<>(game.getPlayers().values());
                        int playersWithCards = 0;

                        for (Player player : players) {
                            if (player.getDeckSize() > 0) {
                                playersWithCards += 1;
                            }
                        }

                        // Win Condition #2: Only 1 player has cards remaining
                        if (playersWithCards == 1) {
                            game.setState(GameState.FINISHED);
                            // Start timer for game cleanup
                            game.setLastActivityTimestamp(System.currentTimeMillis());
                        }
                        // Win Condition #3: No players with cards remaining
                        else if (playersWithCards == 0) {
                            game.setState(GameState.FINISHED);
                            // Start timer for game cleanup
                            game.setLastActivityTimestamp(System.currentTimeMillis());
                        }
                        // Default
                        else {
                            game.setState(GameState.DRAWING_CARDS);
                            // Start timer for players to put a card in play
                            game.setLastActivityTimestamp(System.currentTimeMillis());
                        }
                        result.set(0, true);
                        this.publishGameUpdate(gameId);
                    }
                return game;
            }
        });
        return result.get(0);
    }

    /**
     * @deprecated Never called through the frontend.
     * Kept for manual testing via REST call
     * @param gameId
     * @return
     */
    @Deprecated
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
                    this.publishGameUpdate(gameId);
                }
                return game;
            }
        });
        return result.get(0);
    }
    
    /**
     * Processes the results of a clash action for a Game with {@code Game.state == CLASH_PROCESSING_DECISION}.
     * Called automatically by the internal scheduler after a delay.
     * 
     * <p> This method handles: 
     * - Removing defeated cards from play.
     * - Awarding totems for knockouts.
     * - Advancing to the next turn or determining clash winner.
     * </p>
     * 
     * <p> Interprets what to handle via {@code Game.lastAction}:
     * - Both {@code dealingPlayerId} and {@code receivingPlayerId} != "null" ->
     * Player with id={@code dealingPlayerId} attacked Player with id={@code receivingPlayerId}'s Card in play
     * and dealt {@code damageDealt}
     * - {@code dealingPlayerId} != "null" but {@code receivingPlayerId} == "null" ->
     * Player with id={@code dealingPlayerId} chose to skip their turn
     * - {@code dealingPlayerId} == "null" but {@code receivingPlayerId} != "null" ->
     * Player with {@code receivingPlayerId} chose to forfeit from the clash on {@code Game.state == CLASH_PLAYER_REPLACING_CARD}
     * </p>
     * @param gameId Game UUID
     */
    void processClashDecision(String gameId) {
        games.computeIfPresent(gameId, (id, game) -> {
            synchronized (game) {
                if (game.getState() != GameState.CLASH_PROCESSING_DECISION) {
                    return game;
                }

                Action lastAction = game.getLastAction();
                String dealingPlayerId = lastAction.getDealingPlayerId();
                String receivingPlayerId = lastAction.getReceivingPlayerId();

                // Reward dealing Player for knockout
                Card dealingCard = game.getPlayerCardInPlay(dealingPlayerId);
                Player dealingPlayer = game.getPlayer(dealingPlayerId);

                // Verify receivingPlayer points to valid Player -> Attack action taken
                if (
                    dealingPlayerId != null && !dealingPlayerId.equals("null") &&
                    receivingPlayerId != null && !receivingPlayerId.equals("null")
                ){
                    Card receivingCard = game.getPlayerCardInPlay(receivingPlayerId);
                    if (receivingCard != null && receivingCard.getCurHp() <= 0) {
                        // Recipient was knocked out
                        game.removeCardInPlay(receivingPlayerId);

                        if (dealingCard != null && dealingCard.isHasTotem()
                            && dealingCard.getStyle() == CardStyle.ATTACKER)
                        {
                            // Attacker style card picks up knockout with totem = wins clash
                            int sacredStones = dealingPlayer.giveSacredStone();
                            if (sacredStones == 3) {
                                // 3 stones needed to win
                                game.setState(GameState.FINISHED);
                                // Start timer for game cleanup
                                game.setLastActivityTimestamp(System.currentTimeMillis());
                            }
                            else {
                                game.setState(GameState.CLASH_CONCLUDED);
                            }
                        }
                        else {
                            // Give totem to dealing Player's Card, recipient must replace card
                            game.resetCardsTotem();
                            game.playerGiveTotem(dealingPlayerId);
                            game.setState(GameState.CLASH_PLAYER_REPLACING_CARD);
                            // Start timer for player to replace card
                            game.setLastActivityTimestamp(System.currentTimeMillis());
                        }
                    }
                    else {
                        // No knockout, continue in turn order
                        game.determineNextInitiativeValue();
                        game.setState(GameState.CLASH_PLAYER_TURN);
                        // Start timer for next player to take action
                        game.setLastActivityTimestamp(System.currentTimeMillis());
                    }
                }
                // Dealing player exists & receivingPlayerId == "null" -> Player skipped
                else if (
                    dealingPlayerId != null && !dealingPlayerId.equals("null") &&
                    receivingPlayerId != null && receivingPlayerId.equals("null")
                ) {
                    game.determineNextInitiativeValue();
                    game.setState(GameState.CLASH_PLAYER_TURN);
                    // Start timer for next player to take action
                    game.setLastActivityTimestamp(System.currentTimeMillis());
                }
                // dealingPlayerId == "null" & receiving player exists -> Player forfeit
                else if (
                    dealingPlayerId != null && dealingPlayerId.equals("null") &&
                    receivingPlayerId != null && !receivingPlayerId.equals("null")
                ) {
                    // Player Forfeit - remove from initiative map
                    game.removePlayerFromInitiative(game.getPlayer(receivingPlayerId));
                    
                    // Determine if a player has won the clash
                    if (game.getInitiativeMap().size() == 1) {
                        // Last remaining player won clash - get them from initiative map
                        String winnerId = game.getInitiativeMap().values().iterator().next();
                        Player winner = game.getPlayer(winnerId);
                        int sacredStones = winner.giveSacredStone();
                        if (sacredStones == 3) {
                            // 3 stones needed to win
                            game.setState(GameState.FINISHED);
                            // Start timer for game cleanup
                            game.setLastActivityTimestamp(System.currentTimeMillis());
                        }
                        else {
                            game.setState(GameState.CLASH_CONCLUDED);
                        }
                    }
                    else {
                        // More than 1 player remains active in the clash, continue
                        game.determineNextInitiativeValue();
                        game.setState(GameState.CLASH_PLAYER_TURN);
                        // Start timer for next player to take action
                        game.setLastActivityTimestamp(System.currentTimeMillis());
                    }
                }

                this.publishGameUpdate(gameId);
                return game;
            }
        });
    }

    /**
     * @deprecated Remove soon.
     * @param playerId
     * @return
     */
    @Deprecated
    public boolean playerWonClash(String playerId) {
        Optional<Player> playerOpt = this.playerService.findPlayerById(playerId);
        final List<Boolean> result = new ArrayList<>(1);
        result.add(false);
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();

            String gameId = player.getGameId();
            games.computeIfPresent(gameId, (id, game) -> {
                synchronized (game) {
                    Player playerTransient = game.getPlayer(player.getId());
                    if(game.getCardsInPlay().size() == 1 && game.getPlayerCardInPlay(playerId) != null) {
                        int sacredStones = playerTransient.giveSacredStone();
                        if (sacredStones == 3) {
                            // Player wins game
                            game.setState(GameState.FINISHED);
                        }
                        else {
                            game.setState(GameState.CLASH_CONCLUDED);
                        }
                        result.set(0, true);
                        this.publishGameUpdate(gameId);
                    }
                    return game;
                }
            });
        }

        return result.get(0);
    }
}
