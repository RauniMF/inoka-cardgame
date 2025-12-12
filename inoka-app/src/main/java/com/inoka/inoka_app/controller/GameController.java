package com.inoka.inoka_app.controller;

import org.springframework.web.bind.annotation.RestController;

import com.inoka.inoka_app.model.Card;
import com.inoka.inoka_app.model.Game;
import com.inoka.inoka_app.model.Player;
import com.inoka.inoka_app.model.PlayerEntry;
import com.inoka.inoka_app.service.GameService;
import com.inoka.inoka_app.service.PlayerService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PutMapping;


@RestController
@RequestMapping("/inoka")
public class GameController {

    private final PlayerService playerService;
    private final GameService gameService;

    public GameController(PlayerService playerService, GameService gameService) {
        this.playerService = playerService;
        this.gameService = gameService;
    }

    @GetMapping("/player/all")
    public ResponseEntity<List<PlayerEntry>> getAllPlayers() {
        List<Player> players = playerService.findAllPlayers();
        List<PlayerEntry> pEntries = players.stream().map(PlayerEntry::new).collect(Collectors.toList());
        return ResponseEntity.ok(pEntries);
    }
    
    @GetMapping("/player/find")
    public ResponseEntity<PlayerEntry> getPlayerById(@RequestParam(name = "id") String id) {
        Optional<Player> player = playerService.findPlayerById(id);
        return player.isPresent() ? ResponseEntity.ok(new PlayerEntry(player.get())) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/player/add")
    public ResponseEntity<PlayerEntry> addPlayer(@RequestBody Player player) {
        PlayerEntry pEntry;
        if (player.getId() == "") {
            Player newPlayer = new Player(player.getName());
            pEntry = new PlayerEntry(playerService.addPlayer(newPlayer));
        }
        else {
            pEntry = new PlayerEntry(playerService.addPlayer(player));
        }
        return new ResponseEntity<>(pEntry, HttpStatus.CREATED);
    }

    @PutMapping("/player/update")
    public ResponseEntity<?> updatePlayer(@RequestParam(name = "name") String name, @RequestBody String id) {
        boolean updated = playerService.updatePlayer(id, name);
        return updated ? new ResponseEntity<>(HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/player/remove")
    public ResponseEntity<?> removePlayerById(@RequestParam String id) {
        boolean removed = playerService.removePlayerById(id);
        return removed ? new ResponseEntity<>(HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/player/remove/all")
    public ResponseEntity<?> removeAllPlayers() {
        playerService.removeAllPlayers();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/player/card/all")
    public ResponseEntity<List<Card>> getPlayerDeck(@RequestParam String playerId) {
        Optional<List<Card>> cardCheck = gameService.getPlayerDeck(playerId);
        return cardCheck.isPresent() ? ResponseEntity.ok(cardCheck.get()) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping(value = "/game/create", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createGame(@RequestParam(required = false) String passcode, @RequestBody String id) {
        Player player = playerService.findPlayerById(id).get();
        Game game = gameService.createGame(passcode, player);
        
        if (game == null || game.getId() == null || game.getId().isEmpty()) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body("Error: Could not join game.");
        }
        
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(game.getId());
    }

    @GetMapping("/game/find")
    public ResponseEntity<Game> findGameByGameId(@RequestParam String id) {
        Optional<Game> game = gameService.getGameById(id);
        return game.isPresent() ? ResponseEntity.ok(game.get()) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    
    @GetMapping("/game/all")
    public ResponseEntity<List<Game>> getAllGames() {
        List<Game> games = gameService.getAllGames();
        return ResponseEntity.ok(games);
    }
    
    @GetMapping("/game/players")
    public ResponseEntity<List<Player>> getPlayersInGame(@RequestParam String id) {
        Optional<List<Player>> playersList = gameService.getPlayersInGame(id);
        if (playersList.isPresent()) {
            List<Player> players = playersList.get();
            return ResponseEntity.ok(players);
        }
        else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/player/ready", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> setPlayerReady(@RequestBody String id) {
        boolean success = gameService.setPlayerReady(id);
        if (success) {
            return ResponseEntity.ok("Player readied up.");
        } else {
            return ResponseEntity.status(404).body("Error setting player ready: Player not found.");
        }
    }

    @GetMapping("/game/ready")
    public ResponseEntity<Boolean> allPlayersReady(@RequestParam String id) {
        Optional<Boolean> arePlayersReady = gameService.allPlayersReady(id);
        if (arePlayersReady.isPresent()) {
            if (arePlayersReady.get()) return ResponseEntity.ok(true);
            else return ResponseEntity.status(206).body(false);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    
    @PutMapping(value = "/game/start", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> startGame(@RequestBody String id) {
        boolean result = gameService.setGameStart(id);
        return result ? new ResponseEntity<>(HttpStatus.OK) : new ResponseEntity<>(HttpStatus.CONFLICT);
    }

    @PutMapping(value = "/game/clash/start", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> startClash(@RequestBody String id) {
        boolean result = gameService.setClashStart(id);
        if (result) return new ResponseEntity<>(HttpStatus.OK);
        return ResponseEntity.status(403).body("Unable to set GameState to CLASH_ROLL_INIT.");
    }

    @PutMapping(value = "/game/clash/processed", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> clashProcessed(@RequestBody String id) {
        boolean result = gameService.setClashFinishedProcessing(id);
        if (result) return new ResponseEntity<>(HttpStatus.OK);
        return ResponseEntity.status(403).body("Unable to set GameState to CLASH_PLAYER_TURN.");
    }

    @GetMapping("/player/rollinit")
    public ResponseEntity<Integer> rollInitiativeForPlayer(@RequestParam String id) {
        int result = gameService.rollInitForPlayer(id);
        if (result == -1) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/player/cardInPlay")
    public ResponseEntity<?> removeCardInPlay(@RequestParam String id) {
        boolean result = gameService.removePlayerCardInPlay(id);
        if (result) return new ResponseEntity<>(HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping(value = "/player/gotKnockout", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> playerPickUpKnockout(@RequestBody String id) {
        boolean result = gameService.playerPickUpKnockout(id);
        if (result) return new ResponseEntity<>(HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping(value = "/player/wonClash", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> playerWonClash(@RequestBody String id) {
        boolean result = gameService.playerWonClash(id);
        if (result) return new ResponseEntity<>(HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
