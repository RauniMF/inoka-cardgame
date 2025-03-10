package com.inoka.inoka_app.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name="players")
public class Player implements Serializable{
    @Id
    @Column(name = "play_id", nullable = true, updatable = false, length = 128)
    private String id;
    @Column(name = "play_name", nullable = true, updatable = true, length = 255)
    private String name;
    @Column(name = "game_id", nullable = true, updatable = true, length = 128)
    private String gameId;

    @Transient
    private boolean isReady;
    @Transient
    private List<Card> deck;
    @Transient
    private int sacredStones;
    @Transient
    private int initiative;

    public Player() {
        this.id = UUID.randomUUID().toString();
        this.name = "None";
        this.gameId = "Not in game";
        initializeTransientFields();
    }
    public Player(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.gameId = "Not in game";
        initializeTransientFields();
    }

    private void initializeTransientFields(){
        this.isReady = false;
        this.deck = new ArrayList<>(9); // Deck size of 9
        this.sacredStones = 0;
        this.initiative = rollInitiative();
        generateRandomDeck();
    }

    @PostLoad
    private void postLoad() {
        initializeTransientFields();
    }
    
    public String getId() {
        return id;
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

    public boolean addCardToDeck(Card card) {
        if (this.deck.size() < 9) {
            this.deck.add(card);
            return true;
        } else {
            return false; // Deck is full
        }
    }
    private void generateRandomDeck() {
        Random rand = new Random();
        CardStyle[] styles = CardStyle.values();
        for (int i = 0; i < 9; i++) {
            CardStyle style = styles[rand.nextInt(styles.length)];
            int level = rand.nextInt(3) + 1; // Levels 1, 2, or 3
            Card card = new Card(style, level);
            addCardToDeck(card);
        }
    }
    public List<Card> getDeck() {
        return deck;
    }

    public int getSacredStones() {
        return sacredStones;
    }
    public void setSacredStones(int sacredStones) {
        this.sacredStones = sacredStones;
    }

    public int rollInitiative() {
        Random rand = new Random();
        initiative = rand.nextInt(12) + 1;
        return initiative;
    }
    public int getInitiative() {
        return initiative;
    }
    public void addToInitiative(int modifier) {
        this.initiative += modifier;
    }

    public String getGameId() {
        return gameId;
    }
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }
    public void clearGameId() {
        this.gameId = "Not in game";
    }

    @Override
    public String toString() {
        return "Player [id=" + id + ", name=" + name + ", gameId=" + gameId + "]";
    }
}
