import { Component, Input } from '@angular/core';
import { Player } from '../../player';

@Component({
  selector: 'app-player-entry',
  standalone: true,
  templateUrl: './player-entry.component.html',
  styleUrl: './player-entry.component.css'
})
export class PlayerEntryComponent {
  @Input() player!: Player;
}
