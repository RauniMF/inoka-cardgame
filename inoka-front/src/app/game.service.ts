import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs"
import { Player } from "./player";
import { Card } from "./card";

@Injectable({
    providedIn: 'root'
})
export class GameService {
    private apiServerUrl = 'http://localhost:8080/inoka';

    constructor(private http: HttpClient) { }

    public getAllPlayers(): Observable<Player[]> {
        return this.http.get<Player[]>(`${this.apiServerUrl}/player/all`);
    }

    public findPlayer(id: String): Observable<Player> {
        return this.http.get<Player>(`${this.apiServerUrl}/player/find?id={id}`);
    }

    public addPlayer(player: Player): Observable<Player> {
        return this.http.post<Player>(`${this.apiServerUrl}/player/add`, player);
    }

    public updatePlayer(name: String, id: String): Observable<void> {
        return this.http.put<void>(`${this.apiServerUrl}/player/update?name=${name}`, id);
    }

    public removePlayer(id: String): Observable<void> {
        return this.http.delete<void>(`${this.apiServerUrl}/player/remove?id=${id}`);
    }

    public removeAllPlayers(): Observable<void> {
        return this.http.delete<void>(`${this.apiServerUrl}/player/remove/all`);
    }

    public getPlayerDeck(playerId: String): Observable<Card[]> {
        return this.http.get<Card[]>(`${this.apiServerUrl}/player/card/all?playerId={playerId}`);
    }

    public createGame(playerId: String, passcode: String = ""): Observable<String> {
        if (passcode === "") {
            return this.http.post<String>(`${this.apiServerUrl}/game/create`, playerId);
        }
        else {
            return this.http.post<String>(`${this.apiServerUrl}/game/create?passcode=${passcode}`, playerId);
        }
    }

    public joinGame(playerId: String, passcode: String): Observable<String> {
        return this.http.put<String>(`${this.apiServerUrl}/game/join?passcode={passcode}`, playerId);
    }
}