import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { GameService } from '../../services/game.service';
import { Player } from '../player';
import { Observable, Subscription } from 'rxjs';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { PlayerEntryComponent } from './player-entry/player-entry.component';

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
  
  public player: Player | null = null;
  players: Player[] = [];
  lobbyStatus = signal("Waiting for players");
  private playerSubscription: Subscription | null = null;
  private gameSubscription: Subscription | null = null;

  private startingFlag: boolean = true;

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

        if (this.player?.gameId && this.player.gameId !== 'Not in game') {
          const gameId = this.player.gameId;
    
          // Get all players data for lobby status
          this.gameSubscription = this.gameService.game$.subscribe({
            next: (gameUpdate) => {
              if (gameUpdate) {
                this.players = Array.isArray(gameUpdate.players) ? gameUpdate.players : Object.values(gameUpdate.players);
                this.updateLobbyStatus();
              }
            },
            error: (e) => console.log("Could not fetch Game data in lobby-main: ", e)
          });
        }
      },
      error: (e) => console.error(`Could not fetch player data in lobby-main: `, e)
    });
  }

  /*
   * Responsible for:
   * Updating the lobbyStatus text
   * Starting the game when all players are ready
   */
  updateLobbyStatus(): void {
    if (this.players.length < 2) {
        this.lobbyStatus.set("Waiting for players");
        return;
    }

    /*
     * Check if all players are ready
     * This is done on the game's side in the back-end
     * As we don't store isReady in the Player.ts class 
    */
    if (this.player?.gameId && this.player.gameId !== 'Not in game') {
      const gameId = this.player.gameId;
      this.gameService.allPlayersReady(gameId).subscribe({
        next: (r) => {
          const allReady = r;
          if (allReady && this.startingFlag) this.startGameCooldown();
          else this.lobbyStatus.set("Waiting for all players to be ready...");
        },
        error: (e) => console.error(`Game id:${gameId} error returning player ready status: `, e)

      });
    }
    else {
        // TODO - Handle error detection
        console.error(`Player "${this.player?.name}", id:${this.player?.id} could not update lobby status in lobby-main: Not in game.`);
        return;
    }

  }

  /*
   * Handles setting the player's
   * isReady value in the back-end
   */
  toggleReady(): void {
    if (this.player?.id) {
      this.gameService.setPlayerReady(this.player.id).subscribe({
        next: (r) => {
          this.updateLobbyStatus();
        },
        error: (e) => console.error(`Player "${this.player?.name}", id:${this.player?.id} could not ready: `, e)
      });
    }
  }
  
  startGameCooldown(): void {
    let count = 5;
    this.lobbyStatus.set(`Game starting in: ${count}`);
    this.startingFlag = false;

    const interval = setInterval(() => {
      count--;
      this.lobbyStatus.set(`Game starting in: ${count}`);

      if (count === 0) {
        clearInterval(interval);
        // Navigate to game page
        this.lobbyStatus.set("Game starting...");
        this.gameService.startGame().subscribe({
          next: () => {
            console.log("Game starting.");
            this.router.navigate(["/game"]);
          },
          error: (e) => {
            // 409 Conflict means another player already started the game - this is fine
            if (e.status === 409) {
              console.log("Game already started by another player, navigating to game.");
              this.router.navigate(["/game"]);
            } else {
              console.log("Could not start game in lobby-main: ", e);
            }
          }
        });
      }
    }, 1000);
  }
}
