package com.inoka.inoka_app.model;

public class PlayerEntry {
    private String id;
    private String name;
    private String gameId;

    public PlayerEntry(Player player) {
        this.id = player.getId();
        this.name = player.getName();
        this.gameId = player.getGameId();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    
    public String getGameId() {
        return gameId;
    }
}
