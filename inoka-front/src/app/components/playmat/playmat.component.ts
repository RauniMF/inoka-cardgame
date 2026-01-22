import { Component, inject, Input, OnDestroy, OnInit, signal } from '@angular/core';
import { HandComponent } from "./hand/hand.component";
import { Card } from '../card';
import { CardComponent } from "./card/card.component";
import { GameService } from '../../services/game.service';
import { Subscription } from 'rxjs';
import { Player } from '../player';
import { CommonModule } from '@angular/common';
import { ActionView, Game, GameState, GameView, PlayerView } from '../game';
import { GameWebSocketService } from '../../services/game-websocket.service';

type DropdownData = [number, number, boolean];

@Component({
  selector: 'app-playmat',
  standalone: true,
  templateUrl: './playmat.component.html',
  styleUrl: './playmat.component.css',
  imports: [CommonModule, HandComponent, CardComponent]
})
export class PlaymatComponent implements OnInit, OnDestroy {
  @Input() selectedCard: Card | null = null;
  handSuppressed: boolean = false;

  players: PlayerView[] = [];
  cardsInPlay: Map<number, Card> = new Map(); // Seat # --> Card object
  gameStatus = signal("");
  private prevGameState: GameState | null = null;
  handState: string | null = null;
  // Cards put in play are flipped over (i.e not revealed)
  cardsNotRevealed = true;
  userTurn = false;
  // Data relevant to dynamic dropdown
  dropdownData: DropdownData = [0, 0, false];
  private mySeat: number | null = null;
  selectedPlayerSeat: number | null = null;

  public player: Player | null = null;
  private game: GameView | null = null;
  private playerSubscription: Subscription | null = null;
  private gameSubscription: Subscription | null = null;
  
  private gameService = inject(GameService);
  private gameWebSocketService = inject(GameWebSocketService);

  ngOnInit(): void {
    this.gameService.getPlayerSeat().subscribe({
      next: (seat) => {
        this.mySeat = seat;
        this.fetchCardsInPlay();
      },
      error: (e) => console.error("Error initializing game in playmat component: ", e)
    })
  }

  ngOnDestroy(): void {
    this.playerSubscription?.unsubscribe();
    this.gameSubscription?.unsubscribe();
  }

  fetchCardsInPlay(): void {
    // Get user's player data from service
    this.playerSubscription = this.gameService.player$.subscribe({
      next: (player) => {
        this.player = player;
        // INFO: 
        // console.log("Player loaded: ", player);
        
        if (this.player?.gameId && this.player.gameId !== 'Not in game') {
          this.gameService.getGame().subscribe({
            next: (gameView) => {
              if (gameView && gameView.id === this.player?.gameId) {
                this.game = gameView;
                // INFO:
                // console.log("Game loaded: ", gameView);

                // Try to retrieve the previous state from localStorage
                const savedPrevState = localStorage.getItem(`game_${this.game.id}_prevState_${this.player?.id}`);

                if (this.game.cardsInPlay) {
                  this.cardsInPlay = new Map(Object.entries(this.game.cardsInPlay).map(([key, value]) => [Number(key), value]));

                  if (this.mySeat && this.cardsInPlay.has(this.mySeat)) {
                    const card : Card | undefined = this.cardsInPlay.get(this.mySeat);
                    if (card) {
                      this.selectedCard = card;
                    }
                    // If player has forfeited from clash, or clash is concluded, suppress hand component

                  }
                }

                if (this.game.playerViews) {
                  this.players = Array.isArray(this.game.playerViews) ? this.game.playerViews : Object.values(this.game.playerViews);
                  // INFO:
                  // console.log("Fetched all player data in PlaymatComponent: ", this.otherPlayers());
                }

                if (savedPrevState && Object.values(GameState).includes(savedPrevState as GameState)) {
                  this.prevGameState = savedPrevState as GameState;
                  // Update visuals to represent current game state
                  this.displayStateVisuals(this.prevGameState);
                }
                this.updateGameStatus();
              }
            },
            error: (e) => {
              if (e.status === 404) {
                console.log("Player not in game.");
              }
              else {
                console.log("Error fetching Game details: ", e);
              }
            }
          });

          // Subscribe to WebSocket updates
          this.gameSubscription = this.gameWebSocketService.gameUpdates$.subscribe({
            next: (game) => {
              if (game) {
                this.game = game;
                // Try to retrieve the previous state from localStorage
                const savedPrevState = localStorage.getItem(`game_${game.id}_prevState_${this.player?.id}`);
              
                if (game.cardsInPlay) {
                  this.cardsInPlay = new Map(Object.entries(game.cardsInPlay).map(([key, value]) => [Number(key), value]));
                  // console.log("Obtained all cards in play: ", this.cardsInPlay);

                  if (this.mySeat && this.cardsInPlay.has(this.mySeat)) {
                    const card : Card | undefined = this.cardsInPlay.get(this.mySeat);
                    if (card !== undefined && card !== null) {
                      this.selectedCard = card;
                    }
                    else {
                      this.selectedCard = null;
                    }
                    // If player has forfeited from clash, or clash is concluded, suppress hand component

                  }
                }
                if (game.playerViews) {
                  this.players = Array.isArray(game.playerViews) ? game.playerViews : Object.values(game.playerViews);
                  // console.log("Fetched all player data in PlaymatComponent: ", this.otherPlayers());
                }

                if (savedPrevState && Object.values(GameState).includes(savedPrevState as GameState)) {
                  this.prevGameState = savedPrevState as GameState;
                  // Update visuals to represent current game state
                  this.displayStateVisuals(this.prevGameState);
                }
                this.updateGameStatus();
              }
            },
            error: (e) => console.log("Could not fetch Game data in playmat: ", e)
          });
        }
      },
      error: (e) => console.error(`Could not fetch player data in playmat: `, e)
    });
  }

  thisPlayer(): PlayerView | null {
    return this.players.find(p => p.seat == this.mySeat) ?? null;
  }

  otherPlayers(): PlayerView[] {
    if (!this.player) return this.players;
    return this.players.filter(p => p.seat !== this.mySeat);
  }

  updateGameStatus(): void {
    if (this.game?.state !== this.prevGameState) {
      this.prevGameState = this.game?.state ?? null;

      // Save to localStorage
      if (this.game?.state) {
        localStorage.setItem(`game_${this.game.id}_prevState_${this.player?.id}`, this.game.state);
      }

      this.onStateChange(this.game?.state!);
    }
  }

  // Function is async, allowing us to timeout before updating visuals
  private async onStateChange(state: GameState): Promise<void> {
    switch(state) {
      case GameState.DRAWING_CARDS:
        await this.handleDrawingCards();
        break;
      case GameState.COUNT_DOWN:
        this.startCountdown();
        break;
      case GameState.CLASH_ROLL_INIT:
        await this.handleInitiativeRoll();
        break;
      case GameState.CLASH_PLAYER_TURN:
        await this.handlePlayerTurn();
        break;
      case GameState.CLASH_PROCESSING_DECISION:
        await this.handleClashProcessingDecision();
        break;
      case GameState.CLASH_PLAYER_REPLACING_CARD:
        await this.handlePlayerReplacingCard();
        break;
      case GameState.CLASH_CONCLUDED:
        await this.handleClashConcluded();
        break;
    }
  }

  // Handles player browser reset, providing updated view
  displayStateVisuals(state: GameState): void {
    switch(state){
      case GameState.DRAWING_CARDS:
        if (this.selectedCard == null) {
          this.gameStatus.set("Select a card to put in play.");
        }
        else {
          this.gameStatus.set("Waiting for players...");
        }
        break;
      case GameState.CLASH_ROLL_INIT:
        this.cardsNotRevealed = false;
        // Get player initiative roll from local storage
        const savedInitRoll = localStorage.getItem(`game_${this.game?.id}_initRoll_${this.player?.id}`);
        this.gameStatus.set(`Rolled a ${savedInitRoll} for initiative.`);
        break;
      case GameState.CLASH_PLAYER_TURN:
        this.cardsNotRevealed = false;
        // Determine whether it is user's turn
        if (this.isUserTurn()) {
          this.gameStatus.set("It's your turn.");
        }
        else {
          this.gameStatus.set(`Waiting for ${this.currentPlayer()}'s decision...`);
        }
        break;
      case GameState.CLASH_PLAYER_REPLACING_CARD:
        this.cardsNotRevealed = false;
        if (this.selectedCard === null) {
          // Prompt user to either play new card, or forfeit
          this.gameStatus.set("Put a new card in play? Or forfeit clash?");
        }
        else {
          this.gameStatus.set(`${this.receivedName()} is making a decision...`);
        }
        break;
      case GameState.CLASH_CONCLUDED:
        this.cardsNotRevealed = false;
        this.handSuppressed = true;
        this.gameStatus.set(`${this.winnerName()} won the clash!`);
        break;
    }

  }

  startCountdown(): void {
    let count = 3;
    this.gameStatus.set(`Revealing cards in ${count}...`);

    const interval = setInterval(() => {
      count--;
      this.gameStatus.set(`Revealing cards in ${count}...`);

      if (count === 0) {
        clearInterval(interval);
        this.gameWebSocketService.startClash(this.game!.id);
      }
    }, 1000);
  }

  isUserTurn(): boolean {
    if (this.mySeat == null) {
      return false;
    }
    const curSeat = this.game?.currentPlayerSeat
    return this.mySeat === curSeat;
  }

  currentPlayer(): string {
    if (!this.game || !this.game.initiativeMap) return "";
    const initMap : Map<number, number> = new Map(Object.entries(this.game.initiativeMap).map(([key, value]) => [Number(key), value]));
    const curPlayerSeat: number | undefined = initMap.get(this.game.currentInitiativeValue);

    if (!curPlayerSeat) return "";
    for (const player of this.players) {
      if (player.seat === curPlayerSeat) return player.name;
    }

    return "";
  }

  toggleDropdown(event: MouseEvent, playerSeat: number | undefined): void {
    // Check if user turn
    if (!this.userTurn) return;
    if (playerSeat) {
      // Clicked player to attack
      this.dropdownData = [event.clientX, event.clientY, true];
      this.selectedPlayerSeat = playerSeat;
    }
    else {
      // Clicked off to remove dropdown
      this.dropdownData = [0, 0, false];
      this.selectedPlayerSeat = null;
    }
  }

  attackCard(event: MouseEvent, receivingPlayerSeat: number): void {
    if (!this.player || !this.userTurn) return;
    this.gameWebSocketService.resolveAction(receivingPlayerSeat);
    this.userTurn = false;
    
    // Reset dropdown data
    this.dropdownData = [0, 0, false];
    this.selectedPlayerSeat = null;
  }

  skipTurn(event: MouseEvent) {
    event.stopPropagation();
    if (!this.player || !this.userTurn) return;
    this.gameWebSocketService.resolveAction(-1);
    this.userTurn = false;

    this.dropdownData = [0, 0, false];
    this.selectedPlayerSeat = null;
  }

  forfeitFromClash(): void {
    if(!this.player || (!this.userTurn && !this.isForfeitButtonPresent())) return;
    this.gameWebSocketService.playerForfeitClash();
    this.userTurn = false;

    this.handSuppressed = true;
    this.selectedCard = null;
    this.dropdownData = [0, 0, false];
    this.selectedPlayerSeat = null;
  }

  interpretClashAction(): string {
    const resolvedAction: ActionView = this.game?.lastAction!;
    
    // Guard against uninitialized or invalid action
    if (!resolvedAction || 
        (resolvedAction.dealingSeat === null && resolvedAction.receivingSeat === null) ||
        resolvedAction.damageDealt === undefined) {
      return "";
    }
    
    let dealingPlayerName: string = "";
    let receivingPlayerName: string = "";

    for (const player of this.players) {
      if (player.seat == resolvedAction.dealingSeat) dealingPlayerName = player.name;
      if (player.seat == resolvedAction.receivingSeat) receivingPlayerName = player.name;
    }

    // Handle forfeit (dealingSeat null but receivingSeat exists)
    if (resolvedAction.dealingSeat === null && receivingPlayerName) {
      return `${receivingPlayerName} forfeited from the clash.`;
    }
    // Handle skip (receivingSeat null but dealingSeat exists)
    if (resolvedAction.receivingSeat === null && dealingPlayerName) {
      return `${dealingPlayerName} skipped their turn.`;
    }
    // Normal damage
    return `${dealingPlayerName} dealt ${resolvedAction.damageDealt} damage to ${receivingPlayerName}!`;
  }

  anyCardsOut(): boolean {
    for (const card of this.cardsInPlay.values()) {
      if (card.curHp <= 0) return true;
    }
    return false;
  }

  // Returns name of receivingPlayerId from previous turn action
  receivedName(): string {
    if (!this.game) return "";

    const resolvedAction: ActionView = this.game.lastAction!;
    let receivingPlayerName: string = "";

    for (const player of this.players) {
      if (player.seat == resolvedAction.receivingSeat) receivingPlayerName = player.name;
    }

    return receivingPlayerName;
  }

  // Returns name of dealingPlayerId from previous turn action or last player remaining
  winnerName(): string {
    if (!this.game) return "";
    // Last player remaining
    if (this.cardsInPlay.size == 1) {
      const winnerSeat: number = this.cardsInPlay.keys().next().value!;
      for (const player of this.players) {
        if (player.seat == winnerSeat) return player.name;
      }
    }

    // Player picked up knockout with Attacker to win clash

    const resolvedAction: ActionView = this.game.lastAction!;
    let dealingPlayerName: string = "";

    for (const player of this.players) {
      if (player.seat == resolvedAction.dealingSeat) dealingPlayerName = player.name;
    }

    return dealingPlayerName;
  }

  // Returns whether or not this user picked up a knockout in the previous action
  userKnockout(): boolean {
    if (!this.player || !this.game) return false;

    const resolvedAction: ActionView = this.game.lastAction!;
    if (resolvedAction.damageDealt < 0) return false;
    // User picked up knockout if receivingSeat's card in play has 0 hp
    let receivingCard: Card | undefined = this.cardsInPlay.get(resolvedAction.receivingSeat!);
    if (receivingCard && receivingCard.curHp > 0) return false;
    
    let dealingPlayerName: string = "";

    for (const player of this.players) {
      if (player.seat == resolvedAction.dealingSeat) dealingPlayerName = player.name;
    }

    return this.player.name === dealingPlayerName;
  }

  lastPlayer(): boolean {
    if (!this.player || this.cardsInPlay.size > 1) return false
    return this.cardsInPlay.has(this.mySeat!);
  }

  isForfeitButtonPresent(): boolean {
    if (!this.game || this.handSuppressed) return false;
    return (this.game.state == GameState.CLASH_PLAYER_REPLACING_CARD) && !this.selectedCard
  }

  // ========== State Handler Methods ==========

  private async handleDrawingCards(): Promise<void> {
    this.cardsNotRevealed = true;
    this.handSuppressed = false;
    if (this.selectedCard) this.selectedCard = null;
    this.displayStateVisuals(GameState.DRAWING_CARDS);
  }

  private async handleInitiativeRoll(): Promise<void> {
    this.cardsNotRevealed = false;
    this.gameStatus.set("Rolling for initiative.");
    this.gameService.rollInitForPlayer().subscribe({
      next: (roll) => {
        this.gameStatus.set(`Rolled a ${roll} for initiative.`);
        // Add roll to local storage
        localStorage.setItem(`game_${this.game?.id}_initRoll_${this.player?.id}`, roll.toString());
        this.displayStateVisuals(GameState.CLASH_ROLL_INIT);
      },
      error: (e) => console.error("Error rolling for initiative: ", e)
    });
  }

  private async handlePlayerTurn(): Promise<void> {
    this.userTurn = this.isUserTurn();
    
    // Server automatically detects last player standing, just display turn info
    this.displayStateVisuals(GameState.CLASH_PLAYER_TURN);
  }

  private async handleClashProcessingDecision(): Promise<void> {
    this.userTurn = false;

    const actionMessage = this.interpretClashAction();
    if (actionMessage) {
      this.gameStatus.set(actionMessage);
      await this.delay(3000);
    }
  }

  private async handlePlayerReplacingCard(): Promise<void> {
    // Verify eliminated card is removed from play
    if (this.selectedCard && this.selectedCard.curHp <= 0) {
      this.selectedCard = null;
    }

    if (this.game?.initiativeMap != undefined && this.selectedCard == null) {
      this.gameStatus.set("Put a new card in play? Or forfeit clash?");
    }
    else {
      this.gameStatus.set(`${this.receivedName()} is making a decision...`);
    }
  }

  private async handleClashConcluded(): Promise<void> {
    /*
     * If a player is prompted to choose a new card while a clash is concluded,
     * suppress ability to choose new card
    */
    this.handSuppressed = true;
    if (this.selectedCard?.curHp! <= 0) this.selectedCard = null;
    
    // Show the final action that ended the clash
    const actionMessage = this.interpretClashAction();
    if (actionMessage) {
      this.gameStatus.set(actionMessage);
      await this.delay(3000);
    }
    
    this.gameStatus.set(`${this.winnerName()} won the clash!`);
    await this.delay(3000);
    // Start next clash
    this.gameStatus.set("Starting new clash...");
    /** @todo Verify behavior, handle finishing game */
    this.gameWebSocketService.startNewClash(this.game?.id!);
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
