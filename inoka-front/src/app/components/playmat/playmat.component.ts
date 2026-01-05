import { Component, inject, Input, OnDestroy, OnInit, signal } from '@angular/core';
import { HandComponent } from "./hand/hand.component";
import { Card } from '../card';
import { CardComponent } from "./card/card.component";
import { GameService } from '../../services/game.service';
import { Subscription } from 'rxjs';
import { Player } from '../player';
import { CommonModule } from '@angular/common';
import { Game, GameState } from '../game';
import { GameWebSocketService } from '../../services/game-websocket.service';
import { GameAction } from '../gameAction';

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

  players: Player[] = [];
  cardsInPlay: Map<string, Card> = new Map();
  gameStatus = signal("");
  private prevGameState: GameState | null = null;
  handState: string | null = null;
  // Cards put in play are flipped over (i.e not revealed)
  cardsNotRevealed = true;
  userTurn = false;
  // Data relevant to dynamic dropdown
  dropdownData: DropdownData = [0, 0, false];
  selectedPlayerId: string | null = null;

  public player: Player | null = null;
  private game: Game | null = null;
  private playerSubscription: Subscription | null = null;
  private gameSubscription: Subscription | null = null;
  
  private gameService = inject(GameService);
  private gameWebSocketService = inject(GameWebSocketService);

  ngOnInit(): void {
    this.fetchCardsInPlay();
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
        
        if (this.player?.gameId && this.player.gameId !== 'Not in game') {
          const playerId = this.player.id;
    
          // Get all players data for lobby status
          this.gameSubscription = this.gameService.game$.subscribe({
            next: (game) => {
              if (game) {
                this.game = game;
                // Try to retrieve the previous state from localStorage
                const savedPrevState = localStorage.getItem(`game_${game.id}_prevState_${this.player?.id}`);
              
                if (game.cardsInPlay) {
                  this.cardsInPlay = new Map(Object.entries(game.cardsInPlay));
                  // console.log("Obtained all cards in play: ", this.cardsInPlay);

                  if (this.cardsInPlay.has(playerId)) {
                    const card : Card | undefined = this.cardsInPlay.get(playerId);
                    if (card) {
                      this.selectedCard = card;
                    }
                    // If player has forfeited from clash, or clash is concluded, suppress hand component

                  }
                }
                if (game.players) {
                  this.players = Array.isArray(game.players) ? game.players : Object.values(game.players);
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

  thisPlayer(): Player | null {
    return this.players.find(p => p.id == this.player?.id) ?? null;
  }

  otherPlayers(): Player[] {
    if (!this.player) return this.players;
    return this.players.filter(p => p.id !== this.player?.id);
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
        //console.log("State reached: Drawing cards.")
        this.cardsNotRevealed = true;
        this.handSuppressed = false;
        if (this.selectedCard) this.selectedCard = null;
        this.displayStateVisuals(state);
        break;
      case GameState.COUNT_DOWN:
        //console.log("State reached: Count down.")
        this.startCountdown();
        break;
      case GameState.CLASH_ROLL_INIT:
        //console.log("State reached: Clash Roll Initiative.")
        this.cardsNotRevealed = false;
        this.gameStatus.set("Rolling for initiative.");
        this.gameService.rollInitForPlayer(this.player?.id!).subscribe({
          next: (roll) => {
            this.gameStatus.set(`Rolled a ${roll} for initiative.`);
            // Add roll to local storage
            localStorage.setItem(`game_${this.game?.id}_initRoll_${this.player?.id}`, roll.toString());
            this.displayStateVisuals(state);
          },
          error: (e) => console.log("Error rolling for initiative: ", e)
        });
        break;
      case GameState.CLASH_PLAYER_TURN:
        // console.log("State reached: Clash Player Turn.")
        // Wait a few seconds before changing display
        await new Promise(resolve => setTimeout(resolve, 3000));
        // If user is last player standing, they win
        if (this.lastPlayer()) {
          console.log("Last player check reached.");
          this.gameService.playerWonClash(this.player?.id!);
        }
        else {
          this.displayStateVisuals(state);
        }
        break;
      case GameState.CLASH_PROCESSING_DECISION:
        //console.log("State reached: Clash Processing Decision.")
        this.userTurn = false;
        // Show choice taken / damage dealt
        this.gameStatus.set(this.interpretClashAction());
        // Wait a few seconds before determining next course of action
        await new Promise(resolve => setTimeout(resolve, 3000));
        if (this.anyCardsOut()) {
          // If card was taken out - remove card & let player make choice
          if (this.selectedCard?.curHp! <= 0) {
            // Remove card from play
            this.gameService.removeCardInPlay(this.player?.id!);
          }
          // If player took a knockout, 
          if (this.userKnockout()) {
            // Receive totem & regain 1d12 hit points
            // console.log("Reached 'picked up knockout'!");
            this.gameWebSocketService.pickedUpKnockout(this.player?.id!);
          }
        }
        else {
          // Let back-end know that decision was processed
          this.gameWebSocketService.clashProcessed(this.game!.id);
        }
        break;
      case GameState.CLASH_PLAYER_REPLACING_CARD:
        //console.log("State reached: Player replacing card.")
        if (this.selectedCard?.curHp! <= 0) {
          // Prompt user to either play new card, or forfeit
          this.selectedCard = null;
          this.gameStatus.set("Put a new card in play? Or forfeit clash?");
        }
        else {
          this.gameStatus.set(`${this.receivedName()} is making a decision...`);
        }
        break;
      case GameState.CLASH_CONCLUDED:
        /*
         * If a player is prompted to choose a new card while a clash is concluded,
         * suppress ability to choose new card
        */
        this.handSuppressed = true;
        if (this.selectedCard?.curHp! <= 0) this.selectedCard = null;
        this.gameStatus.set(`${this.winnerName()} won the clash!`);
        await new Promise(resolve => setTimeout(resolve, 3000));
        // Start next clash
        this.gameStatus.set("Starting new clash...");
        this.gameWebSocketService.startNewClash(this.game?.id!);
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
      case GameState.CLASH_PROCESSING_DECISION:
        this.cardsNotRevealed = false;
        // If player card was lost, remove it
        if (this.selectedCard?.curHp! <= 0) {
          // Remove card from play
          this.gameService.removeCardInPlay(this.player?.id!);
        }
        if (this.userKnockout() && !this.selectedCard?.hasTotem) {
          // Receive totem & regain 1d12 hit points
          this.gameWebSocketService.pickedUpKnockout(this.player?.id!);
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
    const userInit: number = +localStorage.getItem(`game_${this.game?.id}_initRoll_${this.player?.id}`)!;
    const curInitVal = this.game?.currentInitiativeValue!
    if (this.game && this.game.state === GameState.CLASH_PLAYER_TURN && userInit == curInitVal) {
      this.userTurn = true;
      return true;
    }
    return false;
  }

  currentPlayer(): string {
    if (!this.game || !this.game.initiativeMap) return "";
    const initMap = new Map(Object.entries(this.game.initiativeMap));
    const curPlayerUUID: string | undefined = initMap.get(this.game.currentInitiativeValue.toString());

    if (!curPlayerUUID) return "";
    for (const player of this.players) {
      if (player.id === curPlayerUUID) return player.name;
    }

    return "";
  }

  toggleDropdown(event: MouseEvent, playerId: string | undefined): void {
    // Check if user turn
    if (!this.userTurn) return;
    if (playerId) {
      // Clicked player to attack
      this.dropdownData = [event.clientX, event.clientY, true];
      this.selectedPlayerId = playerId;
    }
    else {
      // Clicked off to remove dropdown
      this.dropdownData = [0, 0, false];
      this.selectedPlayerId = null;
    }
  }

  attackCard(event: MouseEvent, receivingPlayerId: string): void {
    if (!this.player || !this.userTurn) return;
    this.gameWebSocketService.resolveAction(this.player.id, receivingPlayerId);
    this.userTurn = false;
    
    // Reset dropdown data
    this.dropdownData = [0, 0, false];
    this.selectedPlayerId = null;
  }

  skipTurn(event: MouseEvent) {
    event.stopPropagation();
    if (!this.player || !this.userTurn) return;
    this.gameWebSocketService.resolveAction(this.player.id, "null");
    this.userTurn = false;

    this.dropdownData = [0, 0, false];
    this.selectedPlayerId = null;
  }

  forfeitFromClash(): void {
    if(!this.player || (!this.userTurn && !this.isForfeitButtonPresent())) return;
    this.gameWebSocketService.playerForfeitClash(this.player.id);
    this.userTurn = false;

    this.handSuppressed = true;
    this.selectedCard = null;
    this.dropdownData = [0, 0, false];
    this.selectedPlayerId = null;
  }

  interpretClashAction(): string {
    const resolvedAction: GameAction = this.game?.lastAction!;
    let dealingPlayerName: string = "";
    let receivingPlayerName: string = "";

    for (const player of this.players) {
      if (player.id == resolvedAction.dealingPlayerId) dealingPlayerName = player.name;
      if (player.id == resolvedAction.receivingPlayerId) receivingPlayerName = player.name;
    }

    if (resolvedAction.dealingPlayerId === "null") return `${receivingPlayerName} forfeited from the clash.`
    if (resolvedAction.receivingPlayerId === "null") return `${dealingPlayerName} skipped their turn.`
    return `${dealingPlayerName} dealt ${resolvedAction.damageDealt} damage to ${receivingPlayerName}!`
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

    const resolvedAction: GameAction = this.game.lastAction!;
    let receivingPlayerName: string = "";

    for (const player of this.players) {
      if (player.id == resolvedAction.receivingPlayerId) receivingPlayerName = player.name;
    }

    return receivingPlayerName;
  }

  // Returns name of dealingPlayerId from previous turn action or last player remaining
  winnerName(): string {
    if (!this.game) return "";
    // Last player remaining
    if (this.cardsInPlay.size == 1) {
      const winnerUUID: string = this.cardsInPlay.keys().next().value!;
      for (const player of this.players) {
        if (player.id == winnerUUID) return player.name;
      }
    }

    // Player picked up knockout with Attacker to win clash

    const resolvedAction: GameAction = this.game.lastAction!;
    let dealingPlayerName: string = "";

    for (const player of this.players) {
      if (player.id == resolvedAction.dealingPlayerId) dealingPlayerName = player.name;
    }

    return dealingPlayerName;
  }

  // Returns whether or not this user picked up a knockout in the previous action
  userKnockout(): boolean {
    if (!this.player || !this.game) return false;

    const resolvedAction: GameAction = this.game.lastAction!;
    if (resolvedAction.damageDealt < 0) return false;
    // User picked up knockout if receivingPlayerId's card in play has 0 hp
    let receivingCard: Card | undefined = this.cardsInPlay.get(resolvedAction.receivingPlayerId);
    if (receivingCard && receivingCard.curHp > 0) return false;
    
    let dealingPlayerName: string = "";

    for (const player of this.players) {
      if (player.id == resolvedAction.dealingPlayerId) dealingPlayerName = player.name;
    }

    return this.player.name === dealingPlayerName;
  }

  lastPlayer(): boolean {
    if (!this.player || this.cardsInPlay.size > 1) return false
    return this.cardsInPlay.has(this.player.id);
  }

  isForfeitButtonPresent(): boolean {
    if (!this.game || this.handSuppressed) return false;
    return (this.game.state == GameState.CLASH_PLAYER_REPLACING_CARD) && !this.selectedCard
  }
}
