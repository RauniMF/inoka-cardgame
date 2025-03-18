import { Component, inject, Input } from '@angular/core';
import { HandComponent } from "./hand/hand.component";
import { Card } from '../card';
import { CardComponent } from "./card/card.component";
import { GameService } from '../../services/game.service';
import { Subscription } from 'rxjs';
import { Player } from '../player';

@Component({
  selector: 'app-playmat',
  standalone: true,
  templateUrl: './playmat.component.html',
  styleUrl: './playmat.component.css',
  imports: [HandComponent, CardComponent]
})
export class PlaymatComponent {
  @Input() selectedCard: Card | null = null;
  
  cardsInPlay: Map<string, Card> = new Map();

  public player: Player | null = null;
  private playerSubscription: Subscription | null = null;
  private gameSubscription: Subscription | null = null;
  
  private gameService = inject(GameService);

  ngOnInit(): void {
    this.fetchCardsInPlay();
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
              if (game?.cardsInPlay) {
                this.cardsInPlay = new Map(Object.entries(game.cardsInPlay));

                if (this.cardsInPlay.has(playerId)) {
                  const card : Card | undefined = this.cardsInPlay.get(playerId);
                  if (card) {
                    this.selectedCard = card;
                  }
                }
              }
            },
            error: (e) => console.log("Could not fetch Game data in playmat: ", e)
          });
        }
      },
      error: (e) => console.error(`Could not fetch player data in playmat: `, e)
    });
  }

}
