import {ChangeDetectorRef, Component, inject, Input, SimpleChanges} from '@angular/core';
import { GameService } from '../../game.service';
import { Player } from '../player';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-user',
  template: `
    @if ( player.name !== '') {
        <i>Welcome, {{ player.name }}</i>
    }
    @else {
        Enter a username
    }
    <br/><br/>
    <label for="username">Enter your name:</label><br>
    <input id="username" #nameInput /><br><br>
    <button (click)="updateAndSavePlayer(nameInput.value)">Update</button>
  `,
  styleUrl: './front.component.css',
  standalone: true,
  imports: [CommonModule],
})
export class UserComponent {
    @Input() public username: string = '';
    public player: Player = { name: this.username };
    
    private gameService = inject(GameService);
    private cdr = inject(ChangeDetectorRef);
    
    ngOnInit(): void {
      const storedUUID = localStorage.getItem('userUUID');
      if (storedUUID) {
        this.checkIfPlayerExists(storedUUID);
      }
      else {
        this.addPlayer();
      }
    }

    ngOnChanges(changes: SimpleChanges): void {
      if (changes['username']) {
        this.player.name = this.username;
      }
    }
    
    public addPlayer(): void {
      this.gameService.addPlayer(this.player).subscribe(
        (response: Player) => {
          if (response.id) {
            this.player = response;
            localStorage.setItem('userUUID', response.id);
          }
        });
    }

  public updatePlayer(): void {
    if (this.player.id && this.username) {
      this.gameService.updatePlayer(this.username, this.player.id).subscribe(
        {error: (e) => alert(e.message)})
    }
  }

  public getPlayerID(): string {
    if (this.player.id) {
      return this.player.id;
    }
    else {
      return 'Null';
    }
  }

  public checkIfPlayerExists(id: string): void {
    this.gameService.findPlayer(id).subscribe(
      (response: Player) => {
        this.player = response;
      },
      (error) => {
        this.addPlayer()
      });
  }

  updateAndSavePlayer(newName: string): void {
    this.username = newName;
    this.player.name = this.username;
    localStorage.setItem('username', this.username);
    this.cdr.detectChanges();
    this.updatePlayer();
  }
}