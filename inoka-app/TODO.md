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
- Look at refactoring GameService
     - Currently handles all business logic operations between the controller and repository layers, which includes basic crud operations with the database & higher level game logic
     - Might be beneficial to separate the crud operations into a PlayerService (Done)
- Expand on unit and integration tests
     - Update existing tests to spoof the service layer with mockito (Done)
     - Implement restful api mvc test cases with MockMVC or a similar framework
     - Test security classes
     - More test coverage
- Authentication and Security
     - Implement authentication using JWT for session tokens
     - Store user token in secure cookie
     - Update front-end to send Authorization header in each API call
     - User calls addPlayer and saves UUID to localStorage when first opening webapp
          - **Update addPlayer method and other API calls to require authentication**
     - Refactor WebSocket Game objects to obfuscate sensitive player information like UUID
          - Figure out how to properly identify users via custom IDs in Game object, as well has how to identify the user's Player object in the array using thisPlayer() method in playmat
          - Modify Game object to assign pseudo-IDs when adding Player objects to the game
          - **Implement planned changes**
               - Update SchedulerService, GameController & GameWebSocketController to broadcast GameViews and user-specific decks
               - Front-end model changes (implement DTOs)
               - Update game.service.ts and game-websocket.service.ts to utilize new DTOs
               - Update Hand component to utilize new deckSubscription observable provided by the websocket
               - Modify Playmat component to utilize new DTO to identify players in the view and when sending player action to back-end

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
     - **Taunter class feature**
- Countdown then show all players cards (DONE)
     - Fix clash start / clash processed raising errors (Done)
- Go in initiative order, player chooses move
     - **Cooldown between actions: i.e. show player roll value for some time before moving onto next state** (Done)
     - Player decision: Choose card to attack, skip turn, or forfeit from clash
          - Implement forfeit option (Done)
               - Remove player card & display they've chosen to forfeit on the following CLASH_PROCESSING_DECISION (Done)
- Process the move and move onto next player
     - **Picking up a knockout: Player's card receives totem & regains d12 hp** (Done)
     - Player whose card was knocked out: Can choose to either put another card in play or forfeit from the clash
          - Implement forfeit option
               - Remove player card & remove them from initiative order (Testing required)
               - **Display player chose to forfeit on front-end**
     - Winning a clash: Be the last card standing. Winner receives a sacred stone (Done)
- Process by game state
      - Display each opponent's sacred stone count
      - Display user's sacred stone count
      - Conclude game (finishing screen w/ players ranked by stones obtained)
      - Route back to front page after cleaning up game
- Develop each game state
- Option to leave lobby / game

> Other in-game features
- Class features
     - Attacker: Once per card attack bonus
     - Defender: Once per card healing bonus, heal hp with totem
     - Taunter/Trickster: Charges can be expended to react to oncoming damage, reducing damage.
- Game chat
     - Store and display "logs" i.e. game status messages
     - Player chat
     - Toggle logs