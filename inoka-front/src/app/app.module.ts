import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HttpClient, provideHttpClient, withFetch } from '@angular/common/http';
import { UserComponent } from './user.component';
import { FormsModule } from '@angular/forms';
import { GameService } from './game.service';
import { QueueComponent } from "./queue.component";

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    UserComponent,
    FormsModule,
    QueueComponent
],
  providers: [
    provideHttpClient(withFetch()),
    HttpClient,
    GameService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
