package com.inoka.inoka_app.model;

/**
 * Shorthand description of current Game object state,
 * used in controlling flow of gameplay and state transitions
 */
public enum GameState {
    /**
     * The {@code WAITING_FOR_PLAYERS} state is used in the context of the Game lobby, prior to starting the game proper
     * <p> It indicates the Game is waiting for more players to join and for players in game to signal that they are ready to start </p>
     */
    WAITING_FOR_PLAYERS,
    /**
     * When a game starts (i.e. all players in the lobby have signalled that they are ready), it is set to {@code DRAWING_CARDS}
     * <p> This indicates that players are currently choosing which card to put into play from their hand at the start of the clash </p>
     */
    DRAWING_CARDS,
    /**
     * Brief state between {@code DRAWING_CARDS} and {@code CLASH_ROLL_INIT} at the start of a clash
     * <p> {@code COUNT_DOWN} is used to indicate the countdown after all players have put a card in play has started </p>
     */
    COUNT_DOWN,
    /**
     * At the start of a clash, once all players have put a card in play and after the {@code COUNT_DOWN} state,
     * {@code CLASH_ROLL_INIT} indicates that players should roll for their initiative value
     * <p> Once all players have been added the the Game's initiative map, move onto {@code CLASH_PLAYER_TURN} </p>
     */
    CLASH_ROLL_INIT,
    CLASH_ROLL_HP,
    /**
     * The {@code CLASH_PLAYER_TURN} state indicates the game is waiting to receive an action from the Player in the {@code Game.initativeMap}
     * with an initiative equal to the {@code Game.currentInitiativeValue}
     * <p>
     * A Game's state is set to {@code CLASH_PLAYER_TURN} at the start of the game after {@code CLASH_ROLL_INIT},
     * as well as after {@code CLASH_PROCESSING_DECISION} and {@code CLASH_PLAYER_REPLACING_CARD}
     * when moving onto the next player in the {@code initiativeMap}
     * </p>
     */
    CLASH_PLAYER_TURN,
    /**
     * To provide a delay used to display {@code Game.lastAction} results, the {@code CLASH_PROCESSING_DECISION} state
     * is reached after an action is taken, i.e. after {@code CLASH_PLAYER_TURN}, as well as after {@code CLASH_PLAYER_REPLACING_CARD}
     * if a player chose to forfeit
     */
    CLASH_PROCESSING_DECISION,
    /**
     * After {@code CLASH_PLAYER_TURN}, if resolving the {@code Game.lastAction} resulted in the recipient's Card being knocked out
     * (its hit points dropped below 1), the {@code CLASH_PLAYER_REPLACING_CARD} state is used to indicate that the player whose
     * Card was knocked out may choose whether or not they want to put another Card in play, and which Card they choose
     * <p> If the player chooses to put a card in play, transition state to {@code CLASH_PLAYER_TURN} </p>
     * <p> If the player chooses to forfeit from the clash, transition state to {@code CLASH_PROCESSING_DECISION} </p>
     */
    CLASH_PLAYER_REPLACING_CARD,
    CLASH_TOTEM,
    /**
     * The {@code CLASH_CONCLUDED} state indicates that a clash has concluded, but the game continues.
     * <p> The {@code FINISHED} state is used to indicate a game has concluded. </p>
     * <p> This state is followed by {@code DRAWING_CARDS}, representing the start of a new clash. </p>
     */
    CLASH_CONCLUDED,
    /**
     * The {@code FINISHED} state indicates that a clash was concluded and a player has won the game.
     * <p>
     * Victory can be achieved in the following ways:
     * - A player has received 3 sacred stones by winning 3 clashes
     * - At the start of a clash, if only 1 player has any cards remaining in their hand, they win
     * - At the start of a clash, if no players have any cards in their hand, the player with the most sacred stones wins
     * </p>
     */
    FINISHED
}
