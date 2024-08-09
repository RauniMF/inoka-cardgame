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
    public ResponseEntity<PlayerEntry> getPlayerById(@RequestParam String id) {
        Player player = gameService.findPlayerById(id);
        PlayerEntry pEntry = new PlayerEntry(player);
        return ResponseEntity.ok(pEntry);
    }

    @PostMapping("/player/add")
    public ResponseEntity<PlayerEntry> addPlayer(@RequestBody Player player) {
        Player newPlayer = gameService.addPlayer(player);
        PlayerEntry pEntry = new PlayerEntry(newPlayer);
        return new ResponseEntity<>(pEntry, HttpStatus.CREATED);
    }

    @PutMapping("player/update")
    public ResponseEntity<?> updatePlayer(@RequestParam String name, @RequestBody String id) {
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

    @PostMapping("/game/create")
    public ResponseEntity<String> createGame(@RequestParam(required = false) String passcode, @RequestBody String id) {
        Player player = gameService.findPlayerById(id);
        Game game = gameService.createGame(passcode, player);
        return ResponseEntity.ok(game.getId());
    }

    @PutMapping("/game/join")
    public ResponseEntity<String> joinGame(@RequestParam String passcode, @RequestBody String id) {
        Player player = gameService.findPlayerById(id);
        boolean success = gameService.addPlayerToGame(passcode, player);
        if (success) {
            return ResponseEntity.ok("Player added to game");
        } else {
            return ResponseEntity.status(403).body("Game cannot be joined");
        }
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
    
}
