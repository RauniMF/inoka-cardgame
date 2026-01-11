package com.inoka.inoka_app.service;

import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.inoka.inoka_app.model.Card;
import com.inoka.inoka_app.model.Game;
import com.inoka.inoka_app.model.GameState;
import com.inoka.inoka_app.model.GameView;
import com.inoka.inoka_app.model.Player;
import com.inoka.inoka_app.repositories.PlayerRepository;

@SpringBootTest
public class GameServiceTest {

    // Note to self: constructor injection doesn't work with tests
    // We use the MockitoBean to create a mock for the service
    @MockitoBean
    private PlayerRepository playerRepository;

    @MockitoBean
    private PlayerService playerService;

    @MockitoBean
    private SchedulerService schedulerService;

    @Autowired
    private GameService gameService;
    
    @Test
    public void passcodeLobbyFillTest() {
        /*
         *  Tests many players joining game with same passcode at once.
         *  Front-end service calls createGame with each player & passcode.
         *  We check each returned Game object to see if the results are correct.
         */
        
        // Fill up first lobby with passcode 123
        gameService.createGame("123", new Player("Player One"));
        gameService.createGame("123", new Player("Player Two"));
        gameService.createGame("123", new Player("Player Three"));
        gameService.createGame("123", new Player("Player Four"));
        gameService.createGame("123", new Player("Player Five"));
        Game testOne = gameService.createGame("123", new Player("Player Six"));

        Assertions.assertEquals(testOne.numPlayers(), 6);
        
        // Attempt to join lobby with same passcode
        Game testTwo = gameService.createGame("123", new Player("Player Seven"));

        Assertions.assertEquals(testTwo.numPlayers(), 1);
    }

    @Test
    public void lobbyReadyTest() {
        /*
         *  Tests filling lobby with players
         *  then each player sets their status as ready
         *  and we test if the game states are correct
         */

        Player pOne = new Player("Player One");
        Player pTwo = new Player("Player Two");
        Player pThree = new Player("Player Three");
        Player pFour = new Player("Player Four");
        Player pFive = new Player("Player Five");
        Player pSix = new Player("Player Six");

        Game testGame = gameService.createGame("readyTest", pOne);
        
        pOne.setGameId(testGame.getId());
        pTwo.setGameId(testGame.getId());
        pThree.setGameId(testGame.getId());
        pFour.setGameId(testGame.getId());
        pFive.setGameId(testGame.getId());
        pSix.setGameId(testGame.getId());

        when(playerService.findPlayerById(pOne.getId())).thenReturn(Optional.of(pOne));
        when(playerService.findPlayerById(pTwo.getId())).thenReturn(Optional.of(pTwo));
        when(playerService.findPlayerById(pThree.getId())).thenReturn(Optional.of(pThree));
        when(playerService.findPlayerById(pFour.getId())).thenReturn(Optional.of(pFour));
        when(playerService.findPlayerById(pFive.getId())).thenReturn(Optional.of(pFive));
        when(playerService.findPlayerById(pSix.getId())).thenReturn(Optional.of(pSix));

        Assertions.assertEquals(testGame.getState(), GameState.WAITING_FOR_PLAYERS);

        gameService.createGame("readyTest", pTwo);
        gameService.createGame("readyTest", pThree);
        gameService.createGame("readyTest", pFour);
        gameService.createGame("readyTest", pFive);
        gameService.createGame("readyTest", pSix);
        
        // Asserts all players have been added to testGame object
        Assertions.assertEquals(testGame.numPlayers(), 6);

        // Players aren't ready yet
        Assertions.assertTrue(gameService.allPlayersReady(testGame.getId()).isPresent());
        Assertions.assertFalse(gameService.allPlayersReady(testGame.getId()).get());
        
        // If front-end receives allPlayersReady == true, it calls startGame()
        Assertions.assertFalse(gameService.setGameStart(testGame.getId()));

        Assertions.assertTrue(gameService.setPlayerReady(pOne.getId()));

        Assertions.assertTrue(gameService.getGameById(testGame.getId()).get().getPlayer(pOne.getId()).isReady());

        gameService.setPlayerReady(pTwo.getId());
        gameService.setPlayerReady(pThree.getId());
        gameService.setPlayerReady(pFour.getId());
        gameService.setPlayerReady(pFive.getId());

        // Not all players are ready
        // Assertions.assertFalse(gameService.allPlayersReady(testGame.getId()).get());

        gameService.setPlayerReady(pSix.getId());
        Assertions.assertTrue(gameService.allPlayersReady(testGame.getId()).get());

        // Now startGame() should work
        Assertions.assertTrue(gameService.setGameStart(testGame.getId()));
    }

    @Test
    public void putCardInPlayTest() {
        /*
         *  Tests functionality of getPlayerDeck() method
         *  verifying if Game objects are storing player decks in memory
         *  Then putting a card in play once in a clash
         */

        Player pOne = new Player("Player One");
        Player pTwo = new Player("Player Two");

        Game testGame = gameService.createGame("cardTest", pOne);
        
        pOne.setGameId(testGame.getId());
        pTwo.setGameId(testGame.getId());

        when(playerService.findPlayerById(pOne.getId())).thenReturn(Optional.of(pOne));
        when(playerService.findPlayerById(pTwo.getId())).thenReturn(Optional.of(pTwo));

        Assertions.assertEquals(testGame.getState(), GameState.WAITING_FOR_PLAYERS);

        gameService.createGame("cardTest", pTwo);

        Assertions.assertEquals(testGame.numPlayers(), 2);

        // Players aren't ready yet
        Assertions.assertTrue(gameService.allPlayersReady(testGame.getId()).isPresent());
        Assertions.assertFalse(gameService.allPlayersReady(testGame.getId()).get());

        gameService.setPlayerReady(pOne.getId());
        gameService.setPlayerReady(pTwo.getId());

        // Front-end calls startGame()
        Assertions.assertTrue(gameService.setGameStart(testGame.getId()));
        // No cards in play
        Assertions.assertEquals(gameService.getGameById(testGame.getId()).get().getCardsInPlay().size(), 0);

        // When in game, players receive their hand via the fetchCard() method in the HandComponent
        Assertions.assertEquals(gameService.getPlayerDeck(pOne.getId()).get().size(), 9);

        Card cardToPlay = pOne.getDeck().get(0);

        // The player then chooses a card to put in play, which calls putCardInPlay()
        Assertions.assertTrue(gameService.putCardInPlay(pOne.getId(), cardToPlay));

        // Verify card has been put in play
        Assertions.assertTrue(gameService.getGameById(testGame.getId()).get().getCardsInPlay().size() > 0);
    }

    @Test
    public void seatSelectionTest() {
        /*
         *  Verifies functionality of target selection via seat number,
         *  As well as resolveClashAction() method.
         */

        Player pOne = new Player("Player One");
        Player pTwo = new Player("Player Two");

        Game testGame = gameService.createGame("seatTest", pOne);
        
        pOne.setGameId(testGame.getId());
        pTwo.setGameId(testGame.getId());

        when(playerService.findPlayerById(pOne.getId())).thenReturn(Optional.of(pOne));
        when(playerService.findPlayerById(pTwo.getId())).thenReturn(Optional.of(pTwo));

        Assertions.assertEquals(testGame.getState(), GameState.WAITING_FOR_PLAYERS);

        gameService.createGame("seatTest", pTwo);

        Assertions.assertEquals(testGame.numPlayers(), 2);

        gameService.setPlayerReady(pOne.getId());
        gameService.setPlayerReady(pTwo.getId());

        Assertions.assertTrue(gameService.setGameStart(testGame.getId()));

        Assertions.assertTrue(gameService.putCardInPlay(pOne.getId(), pOne.getDeck().get(0)));
        Assertions.assertTrue(gameService.putCardInPlay(pTwo.getId(), pTwo.getDeck().get(0)));

        Assertions.assertEquals(gameService.getGameById(testGame.getId()).get().getCardsInPlay().size(), 2);

        // Start Clash
        Assertions.assertTrue(gameService.setClashStart(testGame.getId()));

        // Roll for initiative
        Assertions.assertEquals(testGame.getState(), GameState.CLASH_ROLL_INIT);

        int pOneInit = gameService.rollInitForPlayer(pOne.getId());

        // Not all players have rolled yet
        Assertions.assertEquals(testGame.getState(), GameState.CLASH_ROLL_INIT);

        int pTwoInit = gameService.rollInitForPlayer(pTwo.getId());

        Assertions.assertEquals(testGame.getState(), GameState.CLASH_PLAYER_TURN);
    
        Assertions.assertTrue(
            testGame.getCurrentInitiativeValue() == pOneInit ||
            testGame.getCurrentInitiativeValue() == pTwoInit
        );

        String dealingId = "";
        String receivingId = "";
        // One player attacks another
        if (testGame.getCurrentInitiativeValue() == pOneInit) {
            dealingId = pOne.getId();
            receivingId = pTwo.getId();
        }
        else {
            dealingId = pTwo.getId();
            receivingId = pOne.getId();
        }

        // First, check seat number assignment worked
        Assertions.assertEquals(testGame.getSeatForPlayer(pOne.getId()), 1);
        Assertions.assertEquals(testGame.getSeatForPlayer(pTwo.getId()), 2);

        // Then get playerIdBySeat()
        // This mimics what's done by handleClashAction() in GameWebSocketController
        Assertions.assertEquals(
            gameService.getGameByPlayerId(pOne.getId()).get(),
            testGame
        );
        
        Assertions.assertEquals(
            gameService.getGameByPlayerId(pOne.getId()).get()
                .getPlayerIdBySeat(1).get(),
            pOne.getId()
        );

        Assertions.assertTrue(gameService.resolveClashAction(dealingId, receivingId) > 0);
    }

    @Test
    public void emptyPlayerNameTest() {
        /*
         *  Verifies GameView successfully assigns users with no username
         *  a `Player{player.seat}` identifier
         */

        Player pOne = new Player("");

        Game testGame = gameService.createGame("nameTest", pOne);
        
        pOne.setGameId(testGame.getId());

        GameView testGameView = GameView.fromGame(testGame);

        Assertions.assertEquals(testGameView.getPlayerViews().size(), 1);
        Assertions.assertEquals(testGameView.getPlayerViews().get(1).getName(), "Player 1");
    }
}
