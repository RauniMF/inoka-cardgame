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

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    UserComponent,
    FormsModule,
    QueueComponent,
    FrontPageComponent,
    LobbyComponent,
    LobbyMainComponent
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
