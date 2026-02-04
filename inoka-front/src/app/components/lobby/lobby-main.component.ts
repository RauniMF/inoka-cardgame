import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { GameService } from '../../services/game.service';
import { Player } from '../player';
import { Observable, Subscription } from 'rxjs';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { PlayerEntryComponent } from './player-entry/player-entry.component';
import { GameWebSocketService } from '../../services/game-websocket.service';
import { GameState, GameView, PlayerView } from '../game';

@Component({
  selector: 'app-lobby-main',
  standalone: true,
  templateUrl: './lobby-main.component.html',
  styleUrl: './lobby-main.component.css',
  imports: [CommonModule, PlayerEntryComponent]
})
export class LobbyMainComponent implements OnInit, OnDestroy {
  private router = inject(Router);
  private gameService = inject(GameService);
  private gameWebSocketService = inject(GameWebSocketService);
  
  public player: Player | null = null;
  private game: GameView | null = null;
  players: PlayerView[] = [];
  lobbyStatus = signal("Waiting for players");
  private playerSubscription: Subscription | null = null;
  private gameSubscription: Subscription | null = null;

  // Prevents automatically routing upon receiving GameState.DRAWING_CARDS when countdown is active
  private preventRouting: boolean = false;

  ngOnInit(): void {
    this.fetchPlayers();
  }

  ngOnDestroy(): void {
    // Clean up subscriptions
    this.playerSubscription?.unsubscribe();
    this.gameSubscription?.unsubscribe();
  }

  fetchPlayers(): void {
    // Get user's player data from service
    this.playerSubscription = this.gameService.player$.subscribe({
      next: (player) => {
        this.player = player;

        // INFO: 
        console.log("Player loaded: ", player);

        if (this.player?.gameId && this.player.gameId !== 'Not in game') {
          this.gameService.getGame().subscribe({
            next: (gameView) => {
              if (gameView && gameView.id === this.player?.gameId) {
                this.game = gameView;
                // INFO:
                // console.log("Game loaded: ", gameView);
                // Get other players
                if (this.game) {
                  this.players = Array.isArray(this.game.playerViews) ? this.game.playerViews : Object.values(this.game.playerViews);
                  console.log("Players loaded: ", this.players);
                  this.onStateChange(this.game?.state!);
                }
              }
            },
            error: (e) => {
              if (e.status === 404) {
                console.log("Player not in game.");
                this.router.navigate(['/']);
              }
              else {
                console.log("Error fetching Game details: ", e);
              }
            }
          });
        }
        else if (this.player?.gameId && this.player.gameId === 'Not in game') {
          this.router.navigate(['/'])
        }
      },
      error: (e) => console.error(`Could not fetch player data in lobby-main: `, e)
    });

    // Subscribe to WebSocket updates
    this.gameSubscription = this.gameWebSocketService.gameUpdates$.subscribe({
      next: (gameUpdate) => {
        if (gameUpdate) {
          this.game = gameUpdate;
          // INFO:
          // console.log("Game loaded from WebSocket: ", gameUpdate);
          this.players = Array.isArray(gameUpdate.playerViews) ? gameUpdate.playerViews : Object.values(gameUpdate.playerViews);
          this.onStateChange(this.game?.state!);
        }
      },
      error: (e) => console.log("Could not fetch Game data in lobby-main: ", e)
    });
  }

  private onStateChange(state: GameState): void {
    switch(state) {
      case GameState.ALL_PLAYERS_READY:
        this.startGameCooldown();
        break;
      case GameState.DRAWING_CARDS:
        if (!this.preventRouting) this.router.navigate(['/game']);
        break;
      case GameState.WAITING_FOR_PLAYERS:
        if (this.players.length < 2) {
          this.lobbyStatus.set("Waiting for players");
        }
        else {
          this.lobbyStatus.set("Waiting for all players to be ready...");
        }
        break;
    }
  }

  /*
   * Handles setting the player's
   * isReady value in the back-end
   */
  toggleReady(): void {
    if (this.player?.id) {
      this.player.ready = true;
      this.gameService.setPlayerReady().subscribe({
        error: (e) => {
          this.player!.ready = false;
          console.error(`Player "${this.player?.name}", id:${this.player?.id} could not ready: `, e)
        }
      });
    }
  }

  leaveGame(): void {
    if (this.gameSubscription) {
      this.gameSubscription.unsubscribe();
    }
    
    this.game = null;

    this.gameService.leaveGame().subscribe({
      next: () => {
        this.router.navigate(['/'], { replaceUrl: true });
      },
      error: (e) => {
        console.error('Error leaving game: ', e);
        // Navigate away anyway
        this.router.navigate(['/'], { replaceUrl: true });
      }
    })
  }
  
  startGameCooldown(): void {
    let count = 5;
    this.lobbyStatus.set(`Game starting in: ${count}`);
    this.preventRouting = true;

    const interval = setInterval(() => {
      count--;
      this.lobbyStatus.set(`Game starting in: ${count}`);

      if (count === 0) {
        clearInterval(interval);
        // Navigate to game page
        this.lobbyStatus.set("Game starting...");
        this.router.navigate(["/game"]);
      }
    }, 1000);
  }
}
