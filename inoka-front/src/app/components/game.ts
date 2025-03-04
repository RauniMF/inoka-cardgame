import { Player } from "./player";

export enum GameState {
    WAITING_FOR_PLAYERS = "WAITING_FOR_PLAYERS",
    DRAWING_CARDS = "DRAWING_CARDS",
    COUNT_DOWN = "COUNT_DOWN",
    CLASH_ROLL_INIT = "CLASH_ROLL_INIT",
    CLASH_ROLL_HP = "CLASH_ROLL_HP",
    CLASH_PLAYER_TURN = "CLASH_PLAYER_TURN",
    CLASH_PROCESSING_DECISION = "CLASH_PROCESSING_DECISION",
    CLASH_TOTEM = "CLASH_TOTEM",
    CLASH_CONCLUDED = "CLASH_CONCLUDED",
    FINISHED = "FINISHED"
}

export interface Game {
    id: String;
    players: Player[];
    passcode: String;
    state: GameState;
}