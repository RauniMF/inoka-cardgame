import { Component, Input } from "@angular/core";
import { GameService } from "./game.service";

@Component({
    selector: 'app-queue',
    template: ``,
    standalone: true,
    imports: [],
  })
  export class QueueComponent {
    @Input() public passcode: string = '';

    constructor(private gameService: GameService) {}

    joinGame(passcode: string = ''): void {
      const storedUUID = localStorage.getItem('userUUID');
      if (storedUUID) {
        this.gameService.createGame(storedUUID, passcode).subscribe(
          (response: string) => {
            localStorage.setItem("gameID", response)
        });
      }
    }
  }