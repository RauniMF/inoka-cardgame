package com.inoka.inoka_app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Game {
    private String id;
    private List<Player> players;
    private String passcode;
    private GameState state;
    
    public Game() {
        this.id = UUID.randomUUID().toString();
        this.players = new ArrayList<>();
        this.passcode = "";
        this.state = GameState.WAITING_FOR_PLAYERS;
    }

    public Game(String passcode) {
        this.id = UUID.randomUUID().toString();
        this.players = new ArrayList<>();
        this.passcode = passcode;
        this.state = GameState.WAITING_FOR_PLAYERS;
    }

    public String getId() {
        return id;
    }

    public void addPlayer(Player player) {
        if (!players.contains(player)) {
            // Roll new initiative value
            player.rollInitiative();
            // Update player gameId
            player.setGameId(this.id);
            this.players.add(player);
        }
    }
    public List<Player> getPlayers() {
        return players;
    }

    public void updatePasscode(String passcode) {
        this.passcode = passcode;
    }
    public String getPasscode() {
        return passcode;
    }

    public GameState getState() {
        return state;
    }
    public void setState(GameState state) {
        this.state = state;
    }
}
