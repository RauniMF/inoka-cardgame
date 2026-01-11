package com.inoka.inoka_app.controller;

import org.springframework.web.bind.annotation.RestController;

import com.inoka.inoka_app.model.Card;
import com.inoka.inoka_app.model.Game;
import com.inoka.inoka_app.model.GameView;
import com.inoka.inoka_app.model.Player;
import com.inoka.inoka_app.model.PlayerEntry;
import com.inoka.inoka_app.model.PlayerView;
import com.inoka.inoka_app.security.JwtUtil;
import com.inoka.inoka_app.security.PlayerPrincipal;
import com.inoka.inoka_app.service.GameService;
import com.inoka.inoka_app.service.PlayerService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PutMapping;


@RestController
@RequestMapping("/inoka")
public class GameController {

    private final PlayerService playerService;
    private final GameService gameService;
    private final JwtUtil jwtUtil;

    public GameController(
        PlayerService playerService,
        GameService gameService,
        JwtUtil jwtUtil
    ) {
        this.playerService = playerService;
        this.gameService = gameService;
        this.jwtUtil = jwtUtil;
    }

    @Deprecated
    // @GetMapping("/player/all")
    public ResponseEntity<List<PlayerEntry>> getAllPlayers() {
        List<Player> players = playerService.findAllPlayers();
        List<PlayerEntry> pEntries = players.stream().map(PlayerEntry::new).collect(Collectors.toList());
        return ResponseEntity.ok(pEntries);
    }
    
    @GetMapping("/player/find")
    public ResponseEntity<PlayerEntry> getPlayerById(@AuthenticationPrincipal PlayerPrincipal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Player player = principal.getPlayer();
        return ResponseEntity.ok(new PlayerEntry(player));
    }

    @PostMapping("/player/add")
    public ResponseEntity<Map<String, Object>> addPlayer(@RequestBody Player player) {
        PlayerEntry pEntry;
        if (player.getId() == null || player.getId().isEmpty()) {
            Player newPlayer = new Player(player.getName());
            pEntry = new PlayerEntry(playerService.addPlayer(newPlayer));
        }
        else {
            pEntry = new PlayerEntry(playerService.addPlayer(player));
        }

        // Generate JWT token using the player's UUID
        String token = jwtUtil.generateToken(pEntry.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("player", pEntry);
        response.put("token", token);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/player/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, String> request) {
        String playerId = request.get("playerId");
        
        if (playerId == null || playerId.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // Verify player exists in the database
        Optional<Player> playerOpt = playerService.findPlayerById(playerId);
        if (playerOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // Generate new JWT token for existing player
        String newToken = jwtUtil.generateToken(playerId);
        PlayerEntry pEntry = new PlayerEntry(playerOpt.get());

        Map<String, Object> response = new HashMap<>();
        response.put("player", pEntry);
        response.put("token", newToken);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/player/update")
    public ResponseEntity<?> updatePlayer(
        @AuthenticationPrincipal PlayerPrincipal principal,
        @RequestParam(name = "name") String name
    ) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    
        boolean updated = playerService.updatePlayer(principal.getUserId(), name);
        return updated ? new ResponseEntity<>(HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/player/remove")
    public ResponseEntity<?> removePlayerById(@AuthenticationPrincipal PlayerPrincipal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    
        boolean removed = playerService.removePlayerById(principal.getUserId());
        return removed ? new ResponseEntity<>(HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Deprecated
    // @DeleteMapping("/player/remove/all")
    public ResponseEntity<?> removeAllPlayers() {
        playerService.removeAllPlayers();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/player/card/all")
    public ResponseEntity<List<Card>> getPlayerDeck(@AuthenticationPrincipal PlayerPrincipal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    
        Optional<List<Card>> cardCheck = gameService.getPlayerDeck(principal.getUserId());
        return cardCheck.isPresent() ? ResponseEntity.ok(cardCheck.get()) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/game/create")
    public ResponseEntity<String> createGame(
        @AuthenticationPrincipal PlayerPrincipal principal,
        @RequestParam(required = false) String passcode
    ) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    
        Player player = principal.getPlayer();
        Game game = gameService.createGame(passcode, player);
    
        if (game == null || game.getId() == null || game.getId().isEmpty()) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body("Error: Could not join game.");
        }
    
        return ResponseEntity.ok(game.getId());
    }

    @GetMapping("/game/find")
    public ResponseEntity<GameView> findGame(@AuthenticationPrincipal PlayerPrincipal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    
        Player player = principal.getPlayer();
        Optional<Game> gameOpt = gameService.getGameById(player.getGameId());
        
        if (gameOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        GameView gameView = GameView.fromGame(gameOpt.get());
        return ResponseEntity.ok(gameView);
    }
    
    @Deprecated
    // @GetMapping("/game/all")
    public ResponseEntity<List<Game>> getAllGames() {
        List<Game> games = gameService.getAllGames();
        return ResponseEntity.ok(games);
    }
    
    @GetMapping("/game/players")
    public ResponseEntity<List<PlayerView>> getPlayersInGame(@AuthenticationPrincipal PlayerPrincipal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    
        Player player = principal.getPlayer();
        Optional<Game> gameOpt = gameService.getGameById(player.getGameId());
        
        if (gameOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        List<PlayerView> result = new ArrayList<>(GameView.fromGame(gameOpt.get()).getPlayerViews().values());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/player/ready")
    public ResponseEntity<String> setPlayerReady(@AuthenticationPrincipal PlayerPrincipal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    
        Player player = principal.getPlayer();

        boolean success = gameService.setPlayerReady(player.getId());
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
    public ResponseEntity<Integer> rollInitiativeForPlayer(@AuthenticationPrincipal PlayerPrincipal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    
        Player player = principal.getPlayer();

        int result = gameService.rollInitForPlayer(player.getId());
        if (result == -1) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/player/cardInPlay")
    public ResponseEntity<?> removeCardInPlay(@AuthenticationPrincipal PlayerPrincipal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    
        Player player = principal.getPlayer();

        boolean result = gameService.removePlayerCardInPlay(player.getId());
        if (result) return new ResponseEntity<>(HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping("/player/gotKnockout")
    public ResponseEntity<?> playerPickUpKnockout(@AuthenticationPrincipal PlayerPrincipal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    
        Player player = principal.getPlayer();

        boolean result = gameService.playerPickUpKnockout(player.getId());
        if (result) return new ResponseEntity<>(HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping("/player/wonClash")
    public ResponseEntity<?> playerWonClash(@AuthenticationPrincipal PlayerPrincipal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    
        Player player = principal.getPlayer();

        boolean result = gameService.playerWonClash(player.getId());
        if (result) return new ResponseEntity<>(HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/player/seat")
    public ResponseEntity<Integer> getPlayerSeat(@AuthenticationPrincipal PlayerPrincipal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Player player = principal.getPlayer();

        Optional<Game> gameOpt = gameService.getGameByPlayerId(player.getId());
        if (gameOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        int seat = gameOpt.get().getSeatForPlayer(player.getId());
        if (seat == -1) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(seat);
    }
}
