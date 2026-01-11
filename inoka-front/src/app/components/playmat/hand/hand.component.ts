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
  @Output() handStateEmitter = new EventEmitter<HandState>();

  cards: Card[] = [];
  hoveredCard = signal<number | null>(null);
  selectedCard = signal<Card | null>(null);
  handState = signal<HandState>('choosing');
  
  public player: Player | null = null;
  private deckSubscription: Subscription | null = null;

  private gameWebSocketService = inject(GameWebSocketService);
  private gameService = inject(GameService);

  ngOnInit(): void {
    this.fetchCards();
  }

  ngOnDestroy(): void {
    // Clean up subscriptions
    this.deckSubscription?.unsubscribe();
  }

  ngOnChanges(changes: SimpleChanges): void {
    setTimeout(() => {
      if (this.suppressChoosing || (changes['existingCard'] && changes['existingCard'].currentValue)) {
        this.updateState('stowed');
      }
      else if (
        (changes['existingCard'] && !changes['existingCard'].currentValue) ||
        (changes['suppressChoosing'] && !changes['suppressChoosing'].currentValue && !this.existingCard)
      ){
        this.updateState('choosing');
      }
    });
  }

  updateState(state: HandState): void {
    this.handState.set(state);
    // INFO: 
    // console.log("Hand state: ", state);
    this.handStateEmitter.emit(state);
  }

  fetchCards(): void {
    this.gameService.getPlayerDeck().subscribe({
      next: (deck) => {
        if (deck) this.cards = deck;
      },
      error: (e) => {
        console.log("Error fetching cards: ", e);
      }
    });

    // Subscribe to WebSocket updates
    this.deckSubscription = this.gameWebSocketService.deckUpdates$
      .subscribe((deck: Card[] | null) => {
        if (deck) this.cards = deck;
        // INFO:
        console.log("Fetched deck from WebSocket: ", deck);
      });
  }

  onHover(index: number | null): void {
    this.hoveredCard.set(index);
  }

  onSelectCard(card: Card, index: number): void {
    if(this.handState() === 'choosing') {
      this.selectedCard.set(card);
      this.selectedCardEmitter.emit(card);
      this.cards.splice(index, 1);
      this.gameWebSocketService.playCard(card);
    }
  }

  toggleHand(): void {
    if (this.handState() === 'stowed') {
      this.updateState('display');
    }
    else if (this.handState() === 'display') {
      this.updateState('stowed');
    }
  }

  setChoosing(): void {
    if (this.handState() != 'choosing') {
      this.updateState('choosing');
    }
  }

  minimizeHandClick(event: Event): void {
    if (this.handState() === 'display' && !(event.target as HTMLElement).closest('.hand-container')) {
      this.updateState('stowed');
    }
  }
}
