package com.inoka.inoka_app.model;

public class Action {
    private String dealingPlayerId;
    private String receivingPlayerId;
    private int damageDealt;
    
    

    public Action() {
        this.dealingPlayerId = "null";
        this.receivingPlayerId = "null";
        this.damageDealt = 0;
    }

    public Action(String dealingPlayerId, String receivingPlayerId, int damageDealt) {
        this.dealingPlayerId = dealingPlayerId;
        this.receivingPlayerId = receivingPlayerId;
        this.damageDealt = damageDealt;
    }

    public String getDealingPlayerId() {
        return dealingPlayerId;
    }

    public String getReceivingPlayerId() {
        return receivingPlayerId;
    }

    public int getDamageDealt() {
        return damageDealt;
    }
    
}
