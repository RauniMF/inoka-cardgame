# Inoka Card Game

## Features:

## To-Do:
 > Live database of active users
- Implement database using MySQL (DONE)
- Use Spring to streamline queries (DONE)
- Use Postman to test API (DONE)

> Update back end
- STOMP WebSocket communication (DONE)
- GameWebSocketController message handling (DONE)
- Broadcasts using ScheduledExecutorService in GameService (Done)
- Games map as ConcurrentHashMap + synchronized() implementation for Game object modification (Done)

> Implement front end
- Get page setup for basic implementation
     - Username and join lobby inputs (DONE)
     - Join existing game with passcode (Done, handled by service in backend, making joinGame method redundant)
- Find a way to make it so user is cached, won't make a new user when refreshed on the same browser (Done)
- Join game by passcode (Done)
- Waiting screen showing players in lobby
     - Open lobby page when pressing Join Game button  (DONE)
     - Display waiting players in lobby (Done)
           - Update players list and show their ready status (DONE)
     - Implement ready button (Done)
     - **Check for players present, remove them if not**
     - Show players who are ready (DONE)
     - Game starts when at least 2 players are ready (Done)
           - **Route to game page** (Done)

> Game flow
- Only player Id, name and game Id will be kept on the database.
- Game map will keep track of player decks and other attributes.
- Show hand at start (Done)
- Choose card (Done)
     - Communicate with the back-end to set Card in play (Done)
- Countdown then show all players cards (DONE)
- Go in initiative order, player chooses move
     - **Cooldown between actions: i.e. show player roll value for some time before moving onto next state**
- Process the move and move onto next player
- Process by game state
- Develop each game state
