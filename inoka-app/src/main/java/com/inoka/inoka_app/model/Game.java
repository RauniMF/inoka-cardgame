package com.inoka.inoka_app.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.List;
import java.util.UUID;

public class Game {
    private String id;
    private Map<String, Player> players;
    private String passcode;
    private GameState state;
    private Map<String, Card> cardsInPlay;
    private int addSubDice;
    
    public Game() {
        this.id = UUID.randomUUID().toString();
        this.players = new HashMap<>();
        this.passcode = "";
        this.state = GameState.WAITING_FOR_PLAYERS;
        this.cardsInPlay = new HashMap<>();
        this.addSubDice = 6;
    }

    public Game(String passcode) {
        this.id = UUID.randomUUID().toString();
        this.players = new HashMap<>();
        this.passcode = passcode;
        this.state = GameState.WAITING_FOR_PLAYERS;
        this.cardsInPlay = new HashMap<>();
        this.addSubDice = 6;
    }

    public String getId() {
        return id;
    }

    public void addPlayer(Player player) {
        if (!players.keySet().contains(player.getId())) {
            // Roll new initiative value
            player.rollInitiative();
            // Update player gameId
            player.setGameId(this.id);
            this.players.put(player.getId(), player);
        }
    }
    public Map<String, Player> getPlayers() {
        return players;
    }
    public Player getPlayer(String playerId) {
        return players.get(playerId);
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

    public Map<String, Card> getCardsInPlay() {
        return cardsInPlay;
    }
    public void addCardInPlay(String playerId, Card card) {
        this.cardsInPlay.put(playerId, card);
    }

    public void setAddSubDice(int diceSize) {
        this.addSubDice = diceSize;
    }

    /*
     * Given the UUID of player dealing damage & UUID of player receiving damage,
     * Obtains transient Card data in cardsInPlay
     * then calculates and deals damage to receiving card,
     * storing the resulting transient Card data
     */
    public void dealDamage(String dealingPlayerId, String receivingPlayerId) {
        Card dealingCard = this.cardsInPlay.get(dealingPlayerId);
        Card receivingCard = this.cardsInPlay.get(receivingPlayerId);

        // Determine CardStyle matchup
        boolean isPositive = false;
        boolean isNegative = false;
        // Attacker -> Trickster -> Defender -> Attacker
        switch(dealingCard.getStyle()) {
            case ATTACKER: {
                if (receivingCard.getStyle() == CardStyle.TRICKSTER) isPositive = true;
                if (receivingCard.getStyle() == CardStyle.DEFENDER) isNegative = true;
            }
            case DEFENDER: {
                if (receivingCard.getStyle() == CardStyle.ATTACKER) isPositive = true;
                if (receivingCard.getStyle() == CardStyle.TRICKSTER) isNegative = true;
            }
            case TRICKSTER: {
                if (receivingCard.getStyle() == CardStyle.DEFENDER) isPositive = true;
                if (receivingCard.getStyle() == CardStyle.ATTACKER) isNegative = true;
            }
        }

        Random rand = new Random();

        int damage = 0;
        damage += rand.nextInt(8) + 1;
        // Attackers deal additional damage equal to their mark level
        if (dealingCard.getStyle() == CardStyle.ATTACKER) damage += dealingCard.getLevel();
        // Matchup roll: +/- damage based on matchup
        if (isPositive) damage += rand.nextInt(this.addSubDice) + 1;
        if (isNegative) damage -= rand.nextInt(this.addSubDice) + 1;
        // Cannot deal less than 0 damage
        if (damage < 0) damage = 0;

        receivingCard.remCurHp(damage);
        this.cardsInPlay.put(receivingPlayerId, receivingCard);
    }
}
