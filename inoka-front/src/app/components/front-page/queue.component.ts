import { ChangeDetectorRef, Component, inject, Input, OnDestroy, OnInit } from "@angular/core";
import { GameService } from "../../game.service";
import { Player } from "../player";
import { Subscription } from "rxjs";

@Component({
    selector: 'app-queue',
    template: `
      <label for="passcode">Join a game:</label><br><br>
      <input id="passcode" placeholder="Passcode (optional)" #passInput /><br><br>
      <button (click)="joinGame(passInput.value)">Join Game</button>
    `,
    styleUrl: './front.component.css',
    standalone: true,
})
export class QueueComponent implements OnInit, OnDestroy {
  @Input() public passcode: string = '';

  private gameService = inject(GameService);
  private cdr = inject(ChangeDetectorRef);

  public player: Player | null = null;
  private playerSubscription: Subscription | undefined;

  ngOnInit(): void {
    // Subscribe to player changes
    this.playerSubscription = this.gameService.player$.subscribe(
      (player) => {
        this.player = player;
        this.cdr.detectChanges();
      }
    );
  }

  ngOnDestroy(): void {
    this.playerSubscription?.unsubscribe(); // Clean up subscription
  }

  joinGame(passcode: string = ''): void {
    if (!this.player?.id) {
      console.warn("No player found. Please register first.");
      return;
    }

    this.gameService.createGame(this.player.id, passcode)
  }

}