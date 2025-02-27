enum CardStyle {
    ATTACKER,
    DEFENDER,
    TRICKSTER
}

export interface Card {
    style: CardStyle;
    level: number;
    maxHp: number;
    curHp: number;
    hasTotem: boolean;
    taunterCharges: number;
}