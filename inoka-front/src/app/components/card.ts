enum CardStyle {
    ATTACKER = "ATTACKER",
    DEFENDER = "DEFENDER",
    TRICKSTER = "TRICKSTER"
}

export interface Card {
    style: CardStyle;
    level: number;
    maxHp: number;
    curHp: number;
    hasTotem: boolean;
    taunterCharges: number;
}