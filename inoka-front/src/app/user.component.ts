import {Component, input, Input, SimpleChanges} from '@angular/core';
import { GameService } from './game.service';
import { Player } from './player';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-user',
  template: `
    @if ( player.name != '') {
        <p>Your username is {{ player.name }}</p>
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
        this.addPlayer();
      }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['username']) {
          this.player.name = this.username;
        }
      }
    
    public addPlayer(): void {
        this.gameService.addPlayer(this.player).subscribe(
            (response: Player) => {
                this.player = response;
            }
        );
    }

    public updatePlayer(): void {
        if (this.player.id && this.username) {
            this.gameService.updatePlayer(this.username, this.player.id).subscribe(
                {error: (e) => alert(e.message)}
            )
        }
    }
}