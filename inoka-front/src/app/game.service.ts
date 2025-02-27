import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs"
import { Player } from "./components/player";
import { Card } from "./components/card";

@Injectable({
    providedIn: 'root'
})
export class GameService {
    private apiServerUrl = 'http://localhost:8080/inoka';

    constructor(private http: HttpClient) { }

    public getAllPlayers(): Observable<Player[]> {
        return this.http.get<Player[]>(`${this.apiServerUrl}/player/all`);
    }

    public findPlayer(id: string): Observable<Player> {
        return this.http.get<Player>(`${this.apiServerUrl}/player/find?id=${id}`);
    }

    public addPlayer(player: Player): Observable<Player> {
        return this.http.post<Player>(`${this.apiServerUrl}/player/add`, player);
    }

    public updatePlayer(name: string, id: string): Observable<void> {
        return this.http.put<void>(`${this.apiServerUrl}/player/update?name=${name}`, id);
    }

    public removePlayer(id: string): Observable<void> {
        return this.http.delete<void>(`${this.apiServerUrl}/player/remove?id=${id}`);
    }

    public removeAllPlayers(): Observable<void> {
        return this.http.delete<void>(`${this.apiServerUrl}/player/remove/all`);
    }

    public getPlayerDeck(playerId: string): Observable<Card[]> {
        return this.http.get<Card[]>(`${this.apiServerUrl}/player/card/all?playerId={playerId}`);
    }

    public createGame(playerId: string, passcode: string = ""): Observable<string> {
        if (passcode === "") {
            return this.http.post<string>(`${this.apiServerUrl}/game/create`, playerId);
        }
        else {
            return this.http.post<string>(`${this.apiServerUrl}/game/create?passcode=${passcode}`, playerId);
        }
    }

    public joinGame(playerId: string, passcode: string): Observable<string> {
        return this.http.put<string>(`${this.apiServerUrl}/game/join?passcode=${passcode}`, playerId);
    }
}