package com.inoka.inoka_app.model;

public class PlayerEntry {
    private String id;
    private String name;
    private String gameId;
    private boolean isReady;
    private int sacredStones;

    public PlayerEntry(Player player) {
        this.id = player.getId();
        this.name = player.getName();
        this.gameId = player.getGameId();
        this.isReady = player.isReady();
        this.sacredStones = player.getSacredStones();
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

    public boolean isReady() {
        return isReady;
    }

    public int numSacredStones() {
        return sacredStones;
    }
}
