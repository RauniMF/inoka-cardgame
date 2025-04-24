import { Component, computed, Input, signal } from '@angular/core';
import { Card } from '../../card';

@Component({
  selector: 'app-card',
  standalone: true,
  templateUrl: './card.component.html',
  styleUrl: './card.component.css'
})
export class CardComponent {
  @Input() card!: Card;
  @Input() isFlipped: boolean = true;
  isTaunter = computed(() => this.card?.style === "TRICKSTER");
}
