import { Component, EventEmitter, inject, Input, OnChanges, OnDestroy, OnInit, Output, signal, SimpleChanges } from '@angular/core';
import { Card } from '../../card';
import { GameService } from '../../../services/game.service';
import { Player } from '../../player';
import { Subscription } from 'rxjs';
import { CardComponent } from "../card/card.component";
import { GameWebSocketService } from '../../../services/game-websocket.service';

type HandState = 'choosing' | 'stowed' | 'display';

@Component({
  selector: 'app-hand',
  standalone: true,
  templateUrl: './hand.component.html',
  styleUrl: './hand.component.css',
  imports: [CardComponent]
})
export class HandComponent implements OnInit, OnDestroy, OnChanges {
  @Input() existingCard: Card | null = null;
  @Input() suppressChoosing: boolean = false;
  @Output() selectedCardEmitter = new EventEmitter<Card>();

  cards: Card[] = [];
  hoveredCard = signal<number | null>(null);
  selectedCard = signal<Card | null>(null);
  handState = signal<HandState>('choosing');
  
  public player: Player | null = null;
  private playerSubscription: Subscription | null = null;
  private gameSubscription: Subscription | null = null;

  private gameService = inject(GameService);
  private gameWebSocketService = inject(GameWebSocketService);

  ngOnInit(): void {
    this.fetchCards();
  }

  ngOnDestroy(): void {
    // Clean up subscriptions
    this.playerSubscription?.unsubscribe();
    this.gameSubscription?.unsubscribe();
  }

  ngOnChanges(changes: SimpleChanges): void {
      if (this.suppressChoosing || (changes['existingCard'] && changes['existingCard'].currentValue)) {
        this.handState.set('stowed');
      }
      else if (
        (changes['existingCard'] && !changes['existingCard'].currentValue) ||
        (changes['suppressChoosing'] && !changes['suppressChoosing'].currentValue && !this.existingCard)
      ){
        this.handState.set('choosing');
      }
  }

  fetchCards(): void {
    // Get user's player data from service
    this.playerSubscription = this.gameService.player$.subscribe({
      next: (player) => {
        this.player = player;

        if (this.player?.gameId && this.player.gameId !== 'Not in game') {
          const playerId = this.player.id;
    
          // Get all players data for lobby status
          this.gameSubscription = this.gameService.game$.subscribe({
            next: (game) => {
              if (playerId && game?.players && typeof game.players === 'object') {
                const playerTransient = (game.players as Record<string, any>)[playerId];
                if (playerTransient?.deck) {
                  this.cards = playerTransient.deck;
                  // console.log("Cards loaded: ", this.cards);
                }
              }
            },
            error: (e) => console.log("Could not fetch Game data in hand: ", e)
          });
        }
      },
      error: (e) => console.error(`Could not fetch player data in hand: `, e)
    });
  }

  onHover(index: number | null): void {
    this.hoveredCard.set(index);
  }

  onSelectCard(card: Card, index: number): void {
    if(this.handState() === 'choosing' && this.player) {
      this.selectedCard.set(card);
      this.selectedCardEmitter.emit(card);
      this.cards.splice(index, 1);
      this.gameWebSocketService.playCard(this.player.id, card);
    }
  }

  toggleHand(): void {
    if (this.handState() === 'stowed') {
      this.handState.set('display');
    }
    else if (this.handState() === 'display') {
      this.handState.set('stowed');
    }
  }

  setChoosing(): void {
    if (this.handState() != 'choosing') {
      this.handState.set('choosing');
    }
  }

  minimizeHandClick(event: Event): void {
    if (this.handState() === 'display' && !(event.target as HTMLElement).closest('.hand-container')) {
      this.handState.set('stowed');
    }
  }
}
