package com.inoka.inoka_app.model;

import java.util.HashMap;
import java.util.Map;

public class GameView {
    private String id;
    private GameState state;
    // Seat number --> Associated PlayerView
    private Map<Integer, PlayerView> playerViews;
    // Seat number --> Associated player's Card
    private Map<Integer, Card> cardsInPlay;
    private int addSubDice;
    private int currentInitiativeValue;
    // Initiative value --> Seat
    private Map<Integer, Integer> initiativeMap;
    private ActionView lastAction;
    private Integer currentPlayerSeat;
    

    public static GameView fromGame(Game game) {
        GameView view = new GameView();

        view.setId(game.getId());
        view.setState(game.getState());
        view.setAddSubDice(game.getAddSubDice());
        view.setCurrentInitiativeValue(game.getCurrentInitiativeValue());

        // Convert players
        for (Map.Entry<String, Player> entry : game.getPlayers().entrySet()) {
            String playerId = entry.getKey();
            Player player = entry.getValue();
            int seat = game.getSeatForPlayer(playerId);
            view.insertPlayerView(seat, PlayerView.fromPlayer(player, seat));
        }
        
        // Convert cardsInPlay
        for (Map.Entry<String, Card> entry : game.getCardsInPlay().entrySet()) {
            int seat = game.getSeatForPlayer(entry.getKey());
            view.putCardInPlay(seat, entry.getValue());
        }

        // Convert initiativeMap
        for (Map.Entry<Integer, String> entry : game.getInitiativeMap().entrySet()) {
            int seat = game.getSeatForPlayer(entry.getValue());
            view.insertInitiative(entry.getKey(), seat);
        }

        // Convert lastAction
        Action action = game.getLastAction();
        Integer dealingSeat = action.getDealingPlayerId()
            .equals("null") ? null
            : game.getSeatForPlayer(action.getDealingPlayerId());
        Integer receivingSeat = action.getReceivingPlayerId()
            .equals("null") ? null
            : game.getSeatForPlayer(action.getReceivingPlayerId());
        view.setLastAction(new ActionView(dealingSeat, receivingSeat, action.getDamageDealt()));

        // Determine current player seat
        if (game.getCurrentInitiativeValue() != -1) {
            String currentPlayerId = game.getInitiativeMap().get(game.getCurrentInitiativeValue());
            if (currentPlayerId != null) {
                view.setCurrentPlayerSeat(game.getSeatForPlayer(currentPlayerId));
            }
        }

        return view;
    }

    public GameView() {
        this.playerViews = new HashMap<>();
        this.cardsInPlay = new HashMap<>();
        this.initiativeMap = new HashMap<>();
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public Map<Integer, PlayerView> getPlayerViews() {
        return playerViews;
    }
    public void insertPlayerView(int seat, PlayerView playerView) {
        this.playerViews.put(seat, playerView);
    }

    public Map<Integer, Card> getCardsInPlay() {
        return cardsInPlay;
    }
    public void putCardInPlay(int seat, Card card) {
        this.cardsInPlay.put(seat, card);
    }

    public int getAddSubDice() {
        return addSubDice;
    }
    public void setAddSubDice(int addSubDice) {
        this.addSubDice = addSubDice;
    }

    public int getCurrentInitiativeValue() {
        return currentInitiativeValue;
    }
    public void setCurrentInitiativeValue(int currentInitiativeValue) {
        this.currentInitiativeValue = currentInitiativeValue;
    }

    public Map<Integer, Integer> getInitiativeMap() {
        return initiativeMap;
    }
    public void insertInitiative(int initValue, int seat) {
        this.initiativeMap.put(initValue, seat);
    }

    public ActionView getLastAction() {
        return lastAction;
    }
    public void setLastAction(ActionView lastAction) {
        this.lastAction = lastAction;
    }

    public Integer getCurrentPlayerSeat() {
        return currentPlayerSeat;
    }
    public void setCurrentPlayerSeat(Integer currentPlayerSeat) {
        this.currentPlayerSeat = currentPlayerSeat;
    }
}
