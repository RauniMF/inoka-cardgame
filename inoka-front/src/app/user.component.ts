import {Component, input, Input, SimpleChanges} from '@angular/core';
import { GameService } from './game.service';
import { Player } from './player';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-user',
  template: `
    @if ( player.name != '') {
        <i>Welcome, {{ player.name }}</i>
    }
    @else {
        Enter a username
    }
  `,
  standalone: true,
  imports: [CommonModule],
})
export class UserComponent {
    @Input() public username: string = '';
    public player: Player = { name: this.username };
    
    constructor(private gameService: GameService) {}
    
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
}