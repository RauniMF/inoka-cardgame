import { GameAction } from "./gameAction";
import { Card } from "./card";
import { Player } from "./player";

export enum GameState {
    WAITING_FOR_PLAYERS = "WAITING_FOR_PLAYERS",
    DRAWING_CARDS = "DRAWING_CARDS",
    COUNT_DOWN = "COUNT_DOWN",
    CLASH_ROLL_INIT = "CLASH_ROLL_INIT",
    CLASH_ROLL_HP = "CLASH_ROLL_HP",
    CLASH_PLAYER_TURN = "CLASH_PLAYER_TURN",
    CLASH_PROCESSING_DECISION = "CLASH_PROCESSING_DECISION",
    CLASH_PLAYER_REPLACING_CARD = "CLASH_PLAYER_REPLACING_CARD",
    CLASH_TOTEM = "CLASH_TOTEM",
    CLASH_CONCLUDED = "CLASH_CONCLUDED",
    FINISHED = "FINISHED"
}

/**
 *  @deprecated
 *  Use GameView instead.
 */
export interface Game {
    id: string;
    players: Player[];
    passcode: string;
    state: GameState;
    cardsInPlay: Map<string, Card>;
    currentInitiativeValue: number;
    initiativeMap: Map<number, string>;
    lastAction: GameAction;
}

export interface GameView {
    id: string;
    state: string;
    players: { [seat: number]: PlayerView };
    cardsInPlay: { [seat: number]: Card };
    addSubDice: number;
    currentInitiativeValue: number;
    initiativeMap: { [initiative: number]: number }; // Initiative -> Seat
    lastAction: ActionView;
    currentPlayerSeat?: number;
}

export interface PlayerView {
    seat: number;
    name: string;
    isReady: boolean;
    deckSize: number;
    sacredStones: number;
    initiative: number;
}

export interface ActionView {
    dealingSeat?: number;
    receivingSeat?: number;
    damageDealt: number;
}