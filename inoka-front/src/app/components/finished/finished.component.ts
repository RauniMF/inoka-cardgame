import { Component, computed, inject, OnInit, signal, Signal } from '@angular/core';
import { Router } from '@angular/router';
import { PlayerView, PodiumEntry, PodiumView } from '../game';
import { CommonModule } from '@angular/common';

interface PodiumPlacement {
  placement: number;
  player: PlayerView;
  sacredStones: number;
}

@Component({
  selector: 'app-finished',
  standalone: true,
  templateUrl: './finished.component.html',
  styleUrl: './finished.component.css',
  imports: [CommonModule]
})
export class FinishedComponent implements OnInit {
  private podiumView : PodiumView | null = null;
  private podiumEntries : PodiumEntry[] = [];
  top3Entries: Signal<PodiumPlacement[]> = signal([]);
  lowerEntries: Signal<PodiumPlacement[]> = signal([]);

  private router = inject(Router);

  constructor() {
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras.state?.['podiumView']) {
      this.podiumView = navigation.extras.state["podiumView"];
      this.podiumEntries = navigation.extras.state["podiumView"].entries;
      this.compileEntries();
    }
  }

  ngOnInit(): void {
    if (this.podiumView == null || this.podiumEntries.length < 1) {
      this.router.navigate(["/"]);
    }
  }

  private compileEntries() : void {
    let top3Res : PodiumPlacement[] = [];
    let lowerRes : PodiumPlacement[] = [];
    for (const entry of this.podiumEntries) {
      for (const playerView of entry.players) {
        const placement: PodiumPlacement = { placement: entry.placement, player: playerView, sacredStones: entry.sacredStones };
        if (top3Res.length < 3) {
          top3Res.push(placement);
        }
        else {
          lowerRes.push(placement);
        }
      }
    }
    this.top3Entries = computed(() => top3Res);
    this.lowerEntries = computed(() => lowerRes);
  }
}
