# Inoka Card Game

## Features:

## To-Do:
 > Live database of active users
- Implement database using MySQL (DONE)
- Use Spring to streamline queries (DONE)
- Use Postman to test API (DONE)

> Implement front end
- Get page setup for basic implementation
     - Username and join lobby inputs (DONE)
     - Join existing game with passcode (Done, handled by service in backend, making joinGame method redundant)
- Find a way to make it so user is cached, won't make a new user when refreshed on the same browser (Done)
- Join game by passcode (Done)
- Waiting screen showing players in lobby
     - Open lobby page when pressing Join Game button 
     - Display waiting players in lobby
     - Implement ready button
     - Check for players present, remove them if not
     - Show players who are ready
     - Game starts when at least 3 players are ready

> Game flow
- Only player Id, name and game Id will be kept on the database.
- Game map will keep track of player decks and other attributes.
- Show hand at start
- Choose card
- Countdown then show all players cards
- Go in initiative order, player chooses move
- Process the move and move onto next player
- Process by game state
- Develop each game state
