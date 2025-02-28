import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HttpClient, provideHttpClient, withFetch } from '@angular/common/http';
import { UserComponent } from './components/front-page/user.component';
import { FormsModule } from '@angular/forms';
import { GameService } from './game.service';
import { QueueComponent } from "./components/front-page/queue.component";
import { FrontPageComponent } from './components/front-page/front.component';
import { LobbyComponent } from './components/lobby/lobby.component';

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
    LobbyComponent
],
  providers: [
    provideHttpClient(withFetch()),
    HttpClient,
    GameService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
