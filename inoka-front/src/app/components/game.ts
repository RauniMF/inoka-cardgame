import { Player } from "./player";

enum GameState {
    WAITING_FOR_PLAYERS,
    DRAWING_CARDS,
    COUNT_DOWN,
    CLASH_ROLL_INIT,
    CLASH_ROLL_HP,
    CLASH_PLAYER_TURN,
    CLASH_PROCESSING_DECISION,
    CLASH_TOTEM,
    CLASH_CONCLUDED,
    FINISHED
}

export interface Game {
    id: String;
    players: Player[];
    passcode: String;
    state: GameState;
}