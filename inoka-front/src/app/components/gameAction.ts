export interface GameAction {
    targetSeat: number;
    dealingPlayerId: string;
    receivingPlayerId: string;
    damageDealt: number;
}