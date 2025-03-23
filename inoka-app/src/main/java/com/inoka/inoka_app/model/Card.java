package com.inoka.inoka_app.model;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Card {
    private String id;
    private CardStyle style;
    private int level;
    private int maxHp;
    private int curHp;
    private boolean hasTotem;
    private int taunterCharges;

    public Card(@JsonProperty("style") CardStyle style, @JsonProperty("level") int level) {
        this.id = UUID.randomUUID().toString();
        this.style = style;
        this.level = level;
        this.maxHp = this.rollHitDice(level);
        this.curHp = this.maxHp;
        this.hasTotem = false;
        this.taunterCharges = 0;
    }

    private int rollHitDice(int level) {
        Random rand = new Random();
        int hitPoints = 0;
        for (int i = 0; i < level; i++) {
            hitPoints += rand.nextInt(12) + 1 + level;
        }
        if (this.style == CardStyle.DEFENDER) {
            hitPoints += (rand.nextInt(4) + 1) * level;
        }
        return hitPoints;
    }

    public void generateTaunterCharges(int roll) {
        if (roll <= 4) {
            this.taunterCharges = 1;
        }
        else if ((5 <= roll) && (roll <= 8)) {
            this.taunterCharges = 2;
        }
        else if (roll >= 9) {
            this.taunterCharges = 3;
        }
    }
    public boolean hasTaunterCharges() {
        return (taunterCharges > 0);
    }
    public boolean decTaunterCharges() {
        if (taunterCharges < 1) {
            return false;
        } else {
            this.taunterCharges -= 1;
            return true;
        }
    }

    public String getId() {
        return id;
    }

    public CardStyle getStyle() {
        return style;
    }

    public int getLevel() {
        return level;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getCurHp() {
        return curHp;
    }
    public void setCurHp(int curHp) {
        this.curHp = curHp;
    }
    public void remCurHp(int damage) {
        this.curHp -= damage;
    }
    public void addCurHp(int healing) {
        this.curHp += healing;
    }

    public boolean isHasTotem() {
        return hasTotem;
    }
    public int getTaunterCharges() {
        return taunterCharges;
    }
    public void giveTotem() {
        this.hasTotem = true;
    }
    public void takeTotem() {
        this.hasTotem = false;
    }
    
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        Card otherCard = (Card) o;
        return (this.level == otherCard.level) && (this.maxHp == otherCard.maxHp) && Objects.equals(this.style, otherCard.style);
    }
}
