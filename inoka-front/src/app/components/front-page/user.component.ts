import {ChangeDetectorRef, Component, inject, Input, OnDestroy, OnInit, SimpleChanges} from '@angular/core';
import { GameService } from '../../game.service';
import { Player } from '../player';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-user',
  template: `
    @if ( player != null && player.name !== '') {
        <i>Welcome, {{ player.name }}</i>
    }
    @else {
        Enter a username
    }
    <br/><br/>
    <label for="username">Enter your name:</label><br>
    <input id="username" #nameInput /><br><br>
    <button (click)="updatePlayer(nameInput.value)">Update</button>
  `,
  styleUrl: './front.component.css',
  standalone: true,
  imports: [CommonModule],
})
export class UserComponent implements OnInit, OnDestroy {
  @Input() public username: string = '';
  public player: Player | null = null;
  private playerSubscription: Subscription | null = null;
    
  private gameService = inject(GameService);
    
  ngOnInit(): void {
    this.playerSubscription = this.gameService.player$.subscribe(
      (player) => {
        this.player = player;
      }
    );
  }

  ngOnDestroy(): void {
    this.playerSubscription?.unsubscribe(); // Clean up subscription
  }

  public updatePlayer(newName: string): void {
    if (this.player?.id && newName) {
      this.username = newName;
      this.gameService.updatePlayer(this.username);
    }
  }
}