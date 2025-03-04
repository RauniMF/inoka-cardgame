import { ChangeDetectorRef, Component, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { UserComponent } from './user.component';
import { QueueComponent } from './queue.component';
import { GameService } from '../../game.service';
import { Router } from '@angular/router';
import { Game, GameState } from '../game';
import { Subscription } from 'rxjs';

@Component({
    selector: 'app-front',
    templateUrl: './front.component.html',
    styleUrl: './front.component.css',
    standalone: true,
    imports: [UserComponent, QueueComponent],
  })
  export class FrontPageComponent implements OnInit, OnDestroy {
    username: string = '';
    passcode: string = '';
    
    private gameService = inject(GameService);
    private router = inject(Router);
    private gameSubscription: Subscription | null = null;

    constructor(private cdr: ChangeDetectorRef) {}
    @ViewChild(UserComponent) userComponent!: UserComponent;
    @ViewChild(QueueComponent) queueComponent!: QueueComponent;
    
    ngAfterViewInit(): void {
        this.cdr.detectChanges();
    }

    ngOnInit(): void {
      this.gameSubscription = this.gameService.game$.subscribe(
        (game: Game | null) => {
          if (game == null || game?.state === null) return; // Player is not in game

          switch(game.state) {
            case GameState.WAITING_FOR_PLAYERS:
              this.router.navigate(["/lobby"]);
              break;
          }
        }
      );
    }

    ngOnDestroy(): void {
      this.gameSubscription?.unsubscribe();
    }
  }