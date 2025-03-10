import { Component } from '@angular/core';
import { LobbyMainComponent } from "./lobby-main.component";

@Component({
    selector: 'app-lobby',
    templateUrl: './lobby.component.html',
    styleUrl: './lobby.component.css',
    imports: [LobbyMainComponent]
})
export class LobbyComponent {
  
}