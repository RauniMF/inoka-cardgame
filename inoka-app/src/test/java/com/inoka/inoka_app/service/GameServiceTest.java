package com.inoka.inoka_app.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.inoka.inoka_app.model.Game;
import com.inoka.inoka_app.model.GameState;
import com.inoka.inoka_app.model.Player;

@SpringBootTest
public class GameServiceTest {

    // Note to self: constructor injection doesn't work with tests
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

        Assertions.assertTrue(testOne.numPlayers() == 6);
        
        // Attempt to join lobby with same passcode
        Game testTwo = gameService.createGame("123", new Player("Player Seven"));

        Assertions.assertTrue(testTwo.numPlayers() == 1);
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

        Assertions.assertTrue(testGame.getState() == GameState.WAITING_FOR_PLAYERS);

        gameService.createGame("readyTest", pTwo);
        gameService.createGame("readyTest", pThree);
        gameService.createGame("readyTest", pFour);
        gameService.createGame("readyTest", pFive);
        gameService.createGame("readyTest", pSix);
        
        // Asserts all players have been added to testGame object
        Assertions.assertTrue(testGame.numPlayers() == 6);

        // Players aren't ready yet
        Assertions.assertTrue(gameService.allPlayersReady(testGame.getId()).isPresent());
        Assertions.assertFalse(gameService.allPlayersReady(testGame.getId()).get());
        
        // If front-end receives allPlayersReady == true, it calls startGame()


        gameService.setPlayerReady(pOne.getId());
        gameService.setPlayerReady(pTwo.getId());
        gameService.setPlayerReady(pThree.getId());
        gameService.setPlayerReady(pFour.getId());
        gameService.setPlayerReady(pFive.getId());

        // Not all players are ready
        Assertions.assertFalse(gameService.allPlayersReady(testGame.getId()).get());

        gameService.setPlayerReady(pSix.getId());
        Assertions.assertTrue(gameService.allPlayersReady(testGame.getId()).get());

        // Now startGame() should work
        
        
    }
}
