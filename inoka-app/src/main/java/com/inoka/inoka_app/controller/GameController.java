package com.inoka.inoka_app.controller;

import org.springframework.web.bind.annotation.RestController;

import com.inoka.inoka_app.model.Card;
import com.inoka.inoka_app.model.Game;
import com.inoka.inoka_app.model.Player;
import com.inoka.inoka_app.model.PlayerEntry;
import com.inoka.inoka_app.service.GameService;

import io.micrometer.core.ipc.http.HttpSender.Response;

import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.print.attribute.standard.Media;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;




@RestController
@RequestMapping("/inoka")
public class GameController {
    @Autowired
    private GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/player/all")
    public ResponseEntity<List<PlayerEntry>> getAllPlayers() {
        List<Player> players = gameService.findAllPlayers();
        List<PlayerEntry> pEntries = players.stream().map(PlayerEntry::new).collect(Collectors.toList());
        return ResponseEntity.ok(pEntries);
    }
    
    @GetMapping("/player/find")
    public ResponseEntity<PlayerEntry> getPlayerById(@RequestParam(name = "id") String id) {
        Player player = gameService.findPlayerById(id);
        if (player != null) {
            return ResponseEntity.ok(new PlayerEntry(player));
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/player/add")
    public ResponseEntity<PlayerEntry> addPlayer(@RequestBody Player player) {
        PlayerEntry pEntry;
        if (player.getId() == "") {
            Player newPlayer = new Player(player.getName());
            pEntry = new PlayerEntry(gameService.addPlayer(newPlayer));
        }
        else {
            pEntry = new PlayerEntry(gameService.addPlayer(player));
        }
        return new ResponseEntity<>(pEntry, HttpStatus.CREATED);
    }

    @PutMapping("player/update")
    public ResponseEntity<?> updatePlayer(@RequestParam(name = "name") String name, @RequestBody String id) {
        boolean updated = gameService.updatePlayer(id, name);
        if (updated) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/player/remove")
    public ResponseEntity<?> removePlayerById(@RequestParam String id) {
        boolean removed = gameService.removePlayerById(id);
        if (removed) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/player/remove/all")
    public ResponseEntity<?> removeAllPlayers() {
        gameService.removeAllPlayers();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/player/card/all")
    public ResponseEntity<List<Card>> getPlayerDeck(@RequestParam String playerId) {
        Optional<List<Card>> cardCheck = gameService.getPlayerDeck(playerId);
        if (cardCheck.isPresent()) {
            List<Card> cards = cardCheck.get();
            return ResponseEntity.ok(cards);
        }
        else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(value = "/game/create", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createGame(@RequestParam(required = false) String passcode, @RequestBody String id) {
        Player player = gameService.findPlayerById(id);
        Game game = gameService.createGame(passcode, player);
        
        if (game == null || game.getId() == null || game.getId().isEmpty()) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body("Error: Could not join game.");
        }
        
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(game.getId());
    }

    @GetMapping("/game/find")
    public ResponseEntity<Game> findGameByGameId(@RequestParam String id) {
        Game game = gameService.getGame(id);
        return ResponseEntity.ok(game);
    }
    
    @GetMapping("/game/all")
    public ResponseEntity<List<Game>> getAllGames() {
        Map<String, Game> games = gameService.getAllGames();
        return ResponseEntity.ok(games.values().stream().collect(Collectors.toList()));
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
        gameService.setGameStart(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping(value = "/game/clash/start", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> putMethodName(@RequestBody String id) {
        boolean result = gameService.setClashStart(id);
        if (result) return new ResponseEntity<>(HttpStatus.OK);
        return ResponseEntity.status(403).body("Unable to set GameState to CLASH_ROLL_INIT.");
    }

    @GetMapping("player/rollinit")
    public ResponseEntity<Integer> rollInitiativeForPlayer(@RequestParam String id) {
        int result = gameService.rollInitForPlayer(id);
        if (result == -1) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok(result);
    }
}
