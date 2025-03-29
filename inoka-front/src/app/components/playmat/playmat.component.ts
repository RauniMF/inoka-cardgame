import { Component, inject, Input, OnDestroy, OnInit, signal } from '@angular/core';
import { HandComponent } from "./hand/hand.component";
import { Card } from '../card';
import { CardComponent } from "./card/card.component";
import { GameService } from '../../services/game.service';
import { Subscription } from 'rxjs';
import { Player } from '../player';
import { CommonModule } from '@angular/common';
import { Game, GameState } from '../game';

@Component({
  selector: 'app-playmat',
  standalone: true,
  templateUrl: './playmat.component.html',
  styleUrl: './playmat.component.css',
  imports: [CommonModule, HandComponent, CardComponent]
})
export class PlaymatComponent implements OnInit, OnDestroy {
  @Input() selectedCard: Card | null = null;
  
  players: Player[] = [];
  cardsInPlay: Map<string, Card> = new Map();
  gameStatus = signal("");
  private prevGameState: GameState | null = null;
  // Cards put in play are flipped over (i.e not revealed)
  cardsNotRevealed = true;

  public player: Player | null = null;
  private game: Game | null = null;
  private playerSubscription: Subscription | null = null;
  private gameSubscription: Subscription | null = null;
  
  private gameService = inject(GameService);

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
                if (savedPrevState && Object.values(GameState).includes(savedPrevState as GameState)) {
                  this.prevGameState = savedPrevState as GameState;
                }
                this.updateGameStatus();
              }
              if (game?.cardsInPlay) {
                this.cardsInPlay = new Map(Object.entries(game.cardsInPlay));
                // console.log("Obtained all cards in play: ", this.cardsInPlay);

                if (this.cardsInPlay.has(playerId)) {
                  const card : Card | undefined = this.cardsInPlay.get(playerId);
                  if (card) {
                    this.selectedCard = card;
                  }
                }
              }
              if (game?.players) {
                this.players = Array.isArray(game.players) ? game.players : Object.values(game.players);
                // console.log("Fetched all player data in PlaymatComponent: ", this.otherPlayers());
              }
            },
            error: (e) => console.log("Could not fetch Game data in playmat: ", e)
          });
        }
      },
      error: (e) => console.error(`Could not fetch player data in playmat: `, e)
    });
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

    // TODO - Handle player refreshing page

  }

  private onStateChange(state: GameState): void {
    switch(state) {
      case GameState.DRAWING_CARDS:
        if (this.selectedCard == null) {
          this.gameStatus.set("Select a card to put in play.");
        }
        else {
          this.gameStatus.set("Waiting for players...");
        }
        break;
      case GameState.COUNT_DOWN:
        this.startCountdown();
        break;
      case GameState.CLASH_ROLL_INIT:
        this.cardsNotRevealed = false;
        this.gameStatus.set("Rolling for initiative.");
        this.gameService.rollInitForPlayer(this.player?.id!).subscribe({
          next: (roll) => {
            this.gameStatus.set(`Rolled a ${roll} for initiative.`);
          },
          error: (e) => console.log("Error rolling for initiative: ", e)
        });
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
        this.gameService.startClash();
      }
    }, 1000);
  }
}
