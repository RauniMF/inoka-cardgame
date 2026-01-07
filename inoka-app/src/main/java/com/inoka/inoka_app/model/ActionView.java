package com.inoka.inoka_app.model;

public class ActionView {
    private Integer dealingSeat;
    private Integer receivingSeat;
    private int damageDealt;
    
    public ActionView() {}

    public ActionView(Integer dealingSeat, Integer receivingSeat, int damageDealt) {
        this.dealingSeat = dealingSeat;
        this.receivingSeat = receivingSeat;
        this.damageDealt = damageDealt;
    }

    public Integer getDealingSeat() {
        return dealingSeat;
    }
    public void setDealingSeat(Integer dealingSeat) {
        this.dealingSeat = dealingSeat;
    }

    public Integer getReceivingSeat() {
        return receivingSeat;
    }
    public void setReceivingSeat(Integer receivingSeat) {
        this.receivingSeat = receivingSeat;
    }
    
    public int getDamageDealt() {
        return damageDealt;
    }
    public void setDamageDealt(int damageDealt) {
        this.damageDealt = damageDealt;
    }
}
