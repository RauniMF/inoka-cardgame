/** @deprecated Use ActionView in game.ts */
export interface GameAction {
    targetSeat: number;
    dealingPlayerId: string;
    receivingPlayerId: string;
    damageDealt: number;
}