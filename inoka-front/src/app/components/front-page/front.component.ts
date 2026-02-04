import { ChangeDetectorRef, Component, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { UserComponent } from './user.component';
import { QueueComponent } from './queue.component';
import { GameService } from '../../services/game.service';
import { Router } from '@angular/router';
import { Game, GameState, GameView } from '../game';
import { Subscription } from 'rxjs';
import { GameWebSocketService } from '../../services/game-websocket.service';

@Component({
    selector: 'app-front',
    templateUrl: './front.component.html',
    styleUrl: './front.component.css',
    imports: [UserComponent, QueueComponent]
})
  export class FrontPageComponent implements OnInit {
    username: string = '';
    passcode: string = '';
    
    private gameService = inject(GameService);
    private router = inject(Router);
    
    constructor(private cdr: ChangeDetectorRef) {}
    @ViewChild(UserComponent) userComponent!: UserComponent;
    @ViewChild(QueueComponent) queueComponent!: QueueComponent;
    
    ngAfterViewInit(): void {
        this.cdr.detectChanges();
    }

    ngOnInit(): void {
      this.gameService.getGame().subscribe({
        next: (gameView) => {
          if (gameView) {
            console.log('Game loaded: ', gameView);
            this.handleState(gameView.state);
          }
        },
        error: (e) => {
          console.log("Game not found.");
        }
      });
    }

    private handleState(state: GameState): void {
      switch(state) {
        case GameState.FINISHED:
          this.gameService.leaveGame();
          break;
        case GameState.WAITING_FOR_PLAYERS:
        case GameState.ALL_PLAYERS_READY:  
          this.router.navigate(["/lobby"]);
          break;
        case GameState.DRAWING_CARDS:
        case GameState.COUNT_DOWN:
        case GameState.CLASH_ROLL_INIT:
        case GameState.CLASH_PLAYER_TURN:
        case GameState.CLASH_PROCESSING_DECISION:
        case GameState.CLASH_PLAYER_REPLACING_CARD:
        case GameState.CLASH_CONCLUDED:
          this.router.navigate(["/game"]);
          break;
      }
    }
  }