import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HttpClient, provideHttpClient, withFetch } from '@angular/common/http';
import { UserComponent } from './components/front-page/user.component';
import { FormsModule } from '@angular/forms';
import { GameService } from './services/game.service';
import { QueueComponent } from "./components/front-page/queue.component";
import { FrontPageComponent } from './components/front-page/front.component';
import { LobbyComponent } from './components/lobby/lobby.component';
import { LobbyMainComponent } from './components/lobby/lobby-main.component';
import { GameWebSocketService } from './services/game-websocket.service';
import { PlaymatComponent } from './components/playmat/playmat.component';
import { CardComponent } from './components/playmat/card/card.component';
import { HandComponent } from './components/playmat/hand/hand.component';
import { CommonModule } from '@angular/common';
import { PlayerEntryComponent } from './components/lobby/player-entry/player-entry.component';

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    CommonModule,
    BrowserModule,
    AppRoutingModule,
    UserComponent,
    FormsModule,
    QueueComponent,
    FrontPageComponent,
    LobbyComponent,
    LobbyMainComponent,
    PlayerEntryComponent,
    PlaymatComponent,
    CardComponent,
    HandComponent
  ],
  providers: [
    provideHttpClient(withFetch()),
    HttpClient,
    GameService,
    GameWebSocketService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
