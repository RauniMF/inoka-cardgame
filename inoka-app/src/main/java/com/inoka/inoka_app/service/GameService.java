package com.inoka.inoka_app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.inoka.inoka_app.model.Player;
import com.inoka.inoka_app.model.Card;
import com.inoka.inoka_app.model.Game;
import com.inoka.inoka_app.model.GameState;
import com.inoka.inoka_app.repositories.PlayerRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;

@Service
public class GameService {
    @Autowired
    private final PlayerRepository gameRepo;
    // Transient game data stored in HashMap
    private Map<String, Game> games = new HashMap<>();
    
    public GameService(PlayerRepository gameRepo) {
        this.gameRepo = gameRepo;
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
        return gameRepo.findPlayerById(id).orElseThrow(() -> new PlayerNotFoundException ("Player by id " + id + " was not found."));
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

    public Game createGame(String passcode, Player player) {
        if (passcode == null || passcode.isEmpty()) {
            // Check if there's an existing game without a passcode and in WAITING_FOR_PLAYERS state
            for (Game game : games.values()) {
                if ((game.getPasscode() == null || game.getPasscode().isEmpty()) && game.getState() == GameState.WAITING_FOR_PLAYERS) {
                    game.addPlayer(player); // Add the player to the existing game
                    gameRepo.save(player);
                    return game;
                }
            }
        }
        // Check if there's a game with the same passcode and in WAITING_FOR_PLAYERS state
        for (Game game : games.values()) {
            if (game.getPasscode() != null && game.getPasscode().equals(passcode)) {
                if (game.getState() == GameState.WAITING_FOR_PLAYERS) {
                    game.addPlayer(player);
                    gameRepo.save(player);
                    return game;
                }
            }
        }

        // If no suitable game was found, create a new game with the given passcode
        Game game = (passcode != null && !passcode.isEmpty()) ? new Game(passcode) : new Game();
        game.addPlayer(player);
        gameRepo.save(player);
        games.put(game.getId(), game);
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

    public boolean addPlayerToGame(String passcode, Player player) {
        for (Game game : games.values()) {
            if (game.getPasscode() != null && game.getPasscode().equals(passcode)) {
                if (game.getState() == GameState.WAITING_FOR_PLAYERS) {
                    game.addPlayer(player);
                    gameRepo.save(player);
                    return true;
                } else {
                    return false; // Game is not in the correct state
                }
            }
        }
        return false;
    }
}
