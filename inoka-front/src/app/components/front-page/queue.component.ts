import { ChangeDetectorRef, Component, inject, Input } from "@angular/core";
import { GameService } from "../../game.service";
import { Router } from "@angular/router";

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
  export class QueueComponent {
    @Input() public passcode: string = '';

    private gameService = inject(GameService);
    private cdr = inject(ChangeDetectorRef);
    private router = inject(Router);

    joinGame(passcode: string = ''): void {
      this.cdr.detectChanges();
      const storedUUID = localStorage.getItem('userUUID');
      if (storedUUID) {
        this.gameService.createGame(storedUUID, passcode).subscribe(
          (response: string) => {
            localStorage.setItem("gameID", response);
            this.router.navigate(['/lobby']);
        },
          (error) => {
            console.error("Failed to join game: ", error);
          });
      }
      else {
        console.warn("No user UUID found in local storage.");
      }
    }
  
  }