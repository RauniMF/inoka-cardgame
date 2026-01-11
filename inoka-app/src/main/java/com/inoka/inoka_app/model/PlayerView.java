package com.inoka.inoka_app.model;

public class PlayerView {
    private int seat;
    private String name;
    private boolean isReady;
    private int deckSize;
    private int sacredStones;
    private int initiative;


    public static PlayerView fromPlayer(Player player, int seat) {
        PlayerView view = new PlayerView();
        view.setSeat(seat);
        // Empty name --> `Player {seat}`
        if (player.getName() == null || player.getName().isBlank()) {
            view.setName(String.format("Player %d", seat));
        } else { view.setName(player.getName()); }
        view.setReady(player.isReady());
        view.setDeckSize(player.getDeckSize());
        view.setSacredStones(player.getSacredStones());
        view.setInitiative(player.getInitiative());
        return view;
    }

    public PlayerView() {}

    public int getSeat() {
        return seat;
    }
    public void setSeat(int seat) {
        this.seat = seat;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public boolean isReady() {
        return isReady;
    }
    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    public int getDeckSize() {
        return deckSize;
    }
    public void setDeckSize(int deckSize) {
        this.deckSize = deckSize;
    }

    public int getSacredStones() {
        return sacredStones;
    }
    public void setSacredStones(int sacredStones) {
        this.sacredStones = sacredStones;
    }

    public int getInitiative() {
        return initiative;
    }
    public void setInitiative(int initiative) {
        this.initiative = initiative;
    }
}
