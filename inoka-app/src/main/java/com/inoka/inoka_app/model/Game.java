package com.inoka.inoka_app.model;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
    private int currentInitiativeValue;
    private Map<Integer, String> initiativeMap;
    private Action lastAction;
    
    public Game() {
        this.id = UUID.randomUUID().toString();
        this.players = new HashMap<>();
        this.passcode = "";
        this.state = GameState.WAITING_FOR_PLAYERS;
        this.cardsInPlay = new HashMap<>();
        this.addSubDice = 6;
        this.currentInitiativeValue = -1;
        this.initiativeMap = new HashMap<>();
        this.lastAction = new Action();
    }

    public Game(String passcode) {
        this.id = UUID.randomUUID().toString();
        this.players = new HashMap<>();
        this.passcode = passcode;
        this.state = GameState.WAITING_FOR_PLAYERS;
        this.cardsInPlay = new HashMap<>();
        this.addSubDice = 6;
        this.currentInitiativeValue = -1;
        this.initiativeMap = new HashMap<>();
        this.lastAction = new Action();
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
    public Card getPlayerCardInPlay(String playerId) {
        return this.cardsInPlay.get(playerId);
    }
    public Card removeCardInPlay(String playerId) {
        return this.cardsInPlay.remove(playerId);
    }
    // Removes totem from any cards in play
    public void resetCardsTotem() {
        for (String playerUUID : this.cardsInPlay.keySet()) {
            Card card = this.cardsInPlay.get(playerUUID);
            if (card.isHasTotem()) {
                card.takeTotem();
                this.cardsInPlay.put(playerUUID, card);
            }
        }
    }
    // Gives totem to player's card (heals card as well)
    public void playerGiveTotem(String playerId) {
        Card card = this.cardsInPlay.get(playerId);
        card.giveTotem();
        // Heal card
        Random rand = new Random();
        card.addCurHp(rand.nextInt(12) + 1);
        this.cardsInPlay.put(playerId, card);
    }

    public void setAddSubDice(int diceSize) {
        this.addSubDice = diceSize;
    }

    public Map<Integer, String> getInitiativeMap() {
        return initiativeMap;
    }
    /*
     * Given a player,
     * If their initiative value is not in the map, add it and return true
     * else return false
     */ 
    public boolean addPlayerInitiativeToMap(Player player) {
        int playerInitiative = player.getInitiative();
        String playerId = player.getId();
        if (!this.initiativeMap.containsKey(playerInitiative)) {
            if (this.initiativeMap.values().contains(playerId)) {
                // Remove player's previous initiative value.
                Iterator<Map.Entry<Integer, String>> iterator = this.initiativeMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Integer, String> entry = iterator.next();
                    if (entry.getValue().equals(playerId)) {
                        iterator.remove();
                    }
                }
            }
            this.initiativeMap.put(playerInitiative, playerId);
            return true;
        }
        return false;
    }

    public int getCurrentInitiativeValue() {
        return currentInitiativeValue;
    }
    public void resetInitiativeValue() {
        this.currentInitiativeValue = -1;
    }

    // Sets and returns the initiative value of the next player in initiative order
    public int determineNextInitiativeValue() {
        List<Integer> initiatives = Arrays.asList(this.initiativeMap.keySet().toArray(new Integer[0]));
        initiatives.sort(Collections.reverseOrder());
        int previousInitVal = this.currentInitiativeValue;
        // Call on empty initiative map
        if (initiatives.isEmpty()) return -1;
        // First call after constructing Game object
        if (previousInitVal == -1) {
            this.currentInitiativeValue = initiatives.get(0);
            return this.currentInitiativeValue;
        }
        // Previous initiative value is no longer in initiatives list
        if (!initiatives.contains(previousInitVal)) {
            
        }
        // Otherwise:
        int nextIndex = initiatives.indexOf(previousInitVal) + 1;
        if (nextIndex == initiatives.size()) {
            this.currentInitiativeValue = initiatives.get(0);
            return this.currentInitiativeValue;
        }
        this.currentInitiativeValue = initiatives.get(nextIndex);
        return this.currentInitiativeValue;
    }

    public Action getLastAction() {
        return lastAction;
    }
    public void setLastAction(String dealingPlayerId, String receivingPlayerId, int damage) {
        this.lastAction = new Action(dealingPlayerId, receivingPlayerId, damage);
    }
    /*
     * Given the UUID of player dealing damage & UUID of player receiving damage,
     * Obtains transient Card data in cardsInPlay
     * then calculates and deals damage to receiving card,
     * storing the resulting transient Card data
     */
    public int dealDamage(String dealingPlayerId, String receivingPlayerId) {
        Card dealingCard = this.cardsInPlay.get(dealingPlayerId);
        Card receivingCard = this.cardsInPlay.get(receivingPlayerId);

        // Determine CardStyle matchup
        boolean isPositive = false;
        boolean isNegative = false;
        // Attacker -> Trickster -> Defender -> Attacker
        switch(dealingCard.getStyle()) {
            case CardStyle.ATTACKER:
                if (receivingCard.getStyle() == CardStyle.TRICKSTER) isPositive = true;
                if (receivingCard.getStyle() == CardStyle.DEFENDER) isNegative = true;
                break;
            case CardStyle.DEFENDER:
                if (receivingCard.getStyle() == CardStyle.ATTACKER) isPositive = true;
                if (receivingCard.getStyle() == CardStyle.TRICKSTER) isNegative = true;
                break;
            case CardStyle.TRICKSTER:
                if (receivingCard.getStyle() == CardStyle.DEFENDER) isPositive = true;
                if (receivingCard.getStyle() == CardStyle.ATTACKER) isNegative = true;
                break;
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

        receivingCard.removeCurHp(damage);
        this.cardsInPlay.put(receivingPlayerId, receivingCard);
        return damage;
    }
}
