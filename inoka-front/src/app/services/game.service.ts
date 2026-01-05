import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { BehaviorSubject, Observable } from "rxjs"
import { Player } from "../components/player";
import { Card } from "../components/card";
import { Game } from "../components/game";
import { GameWebSocketService } from "./game-websocket.service";

@Injectable({
    providedIn: 'root' // Ensures a singleton instance across the entire application
})
export class GameService {
    private apiServerUrl = 'http://localhost:8080/inoka';

    // BehaviorSubject holds player's data
    private playerSubject = new BehaviorSubject<Player | null>(null);
    public player$ = this.playerSubject.asObservable();

    // Game data if player is in game
    private gameSubject = new BehaviorSubject<Game | null>(null);
    public game$ = this.gameSubject.asObservable();

    constructor(private http: HttpClient, private gameWebSocketService: GameWebSocketService) {
        // Initialize player data when service starts
        console.log("Page loaded");
        this.loadPlayer();

        this.gameWebSocketService.gameUpdates$.subscribe((updatedGame: Game | null) => {
            if (updatedGame) {
                // INFO: 
                console.log("Game update received via WebSocket: ", updatedGame);
                this.gameSubject.next(updatedGame);
            }
        });
    }

    /*
     * Player object methods
    */
    private loadPlayer(): void {
        const storedUUID = localStorage.getItem('userUUID');
        const storedToken = localStorage.getItem('authToken');
        
        if (storedUUID && storedToken) {
            // Find player with ID in back-end (token will be sent via interceptor)
            this.findPlayer(storedUUID).subscribe({
                next: (p) => {
                    this.playerSubject.next(p);
                    console.log("Player loaded from existing session");
                    if(this.playerSubject.value != undefined &&
                        this.playerSubject.value.gameId != null &&
                        this.playerSubject.value.gameId != "Not in game")
                        this.loadGame(this.playerSubject.value.gameId);
                },
                // If findPlayer fails, handle gracefully
                error: (e) => {
                    console.log("Error loading player, attempting token refresh:", e);
                    // The interceptor may have already handled refresh, but if it fails,
                    // we need to clear data and create a new player
                    this.clearAuthData();
                    this.createNewPlayer();
                }
            });
        } else if (storedUUID && !storedToken) {
            // Have UUID but no token - refresh the token
            // INFO: 
            // console.log("UUID found but no token, refreshing...");
            this.refreshToken(storedUUID).subscribe({
                next: (response) => {
                    localStorage.setItem('authToken', response.token);
                    this.playerSubject.next(response.player);
                    // console.log("Token refreshed for existing player");
                    if(response.player.gameId != null && response.player.gameId != "Not in game") {
                        this.loadGame(response.player.gameId);
                    }
                },
                error: (e) => {
                    // Player UUID doesn't exist in database, or refresh failed
                    // console.log("Failed to refresh token, player may not exist. Creating new player.", e);
                    this.clearAuthData();
                    this.createNewPlayer();
                }
            });
        } else {
            // No UUID or token - create new player
            this.createNewPlayer();
        }
    }
    
    private clearAuthData(): void {
        localStorage.removeItem('userUUID');
        localStorage.removeItem('authToken');
    }
    
    private createNewPlayer(): void {
        const newPlayer: Player = { name: "", id: "", ready: false, sacredStones: 0 };
        this.addPlayer(newPlayer).subscribe({
            next: (response) => {
                localStorage.setItem('userUUID', response.player.id);
                localStorage.setItem('authToken', response.token);
                console.log("Player created with token");
                this.playerSubject.next(response.player);
            },
            error: (e) => {
                console.log("Error creating player object: ", e);
            }
        });
    }

    // Fetch game details to update gameSubject
    private loadGame(gameId: string): void {
        this.http.get<Game>(`${this.apiServerUrl}/game/find`).subscribe({
            next: (game) => {
                this.gameSubject.next(game);
                this.gameWebSocketService.connect(game.id);
            },
            error: (e) => console.error("Could not load game: ", e)
        })
    }

    /*
     * Api communication with back-end
    */
    public getAllPlayers(): Observable<Player[]> {
        return this.http.get<Player[]>(`${this.apiServerUrl}/player/all`);
    }

    public findPlayer(id: string): Observable<Player> {
        return this.http.get<Player>(`${this.apiServerUrl}/player/find?id=${id}`);
    }

    public addPlayer(player: Player): Observable<{ player: Player, token: string }> {
        return this.http.post<{ player: Player, token: string }>(`${this.apiServerUrl}/player/add`, player);
    }

    public refreshToken(playerId: string): Observable<{ player: Player, token: string }> {
        return this.http.post<{ player: Player, token: string }>(`${this.apiServerUrl}/player/refresh-token`, { playerId });
    }

    // Updates player's name on backend and the BehaviorSubject
    public updatePlayer(name: string): void {
        const currentPlayer = this.playerSubject.value;
        if (currentPlayer && currentPlayer.id) {
            this.http.put<void>(`${this.apiServerUrl}/player/update?name=${name}`, currentPlayer.id).subscribe({
                next: (r) => {
                    currentPlayer.name = name;
                    this.playerSubject.next(currentPlayer);
                    console.log('PUT request success: ', r);
                },
                error: (e) => console.error("Failed to update player: ", e)
            });
        }
    }

    // Player joins game and updates BehaviorSubject
    public createGame(playerId: string, passcode: string = ""): void {
        const currentPlayer = this.playerSubject.value;
        if (currentPlayer && currentPlayer.id) {
            let httpObservable: Observable<string>;
            if (passcode === "") {
                httpObservable = this.http.post<string>(`${this.apiServerUrl}/game/create`, null, { responseType: 'text' as 'json' });
            }
            else {
                httpObservable = this.http.post<string>(`${this.apiServerUrl}/game/create?passcode=${passcode}`, null, { responseType: 'text' as 'json' });
            }
            httpObservable.subscribe({
                next: (gameId) => {
                    currentPlayer.gameId = gameId;
                    this.playerSubject.next(currentPlayer);
                    console.log('POST request success: Joined game with id=', gameId);
                    this.loadGame(gameId);
                },
                error: (e) => console.error("Failed to create game: ", e)
            });
        }
    }

    public removePlayer(id: string): Observable<void> {
        return this.http.delete<void>(`${this.apiServerUrl}/player/remove?id=${id}`);
    }

    public removeAllPlayers(): Observable<void> {
        return this.http.delete<void>(`${this.apiServerUrl}/player/remove/all`);
    }

    public getPlayerDeck(playerId: string): Observable<Card[]> {
        return this.http.get<Card[]>(`${this.apiServerUrl}/player/card/all?playerId=${playerId}`);
    }

    public joinGame(playerId: string, passcode: string): Observable<string> {
        return this.http.put<string>(`${this.apiServerUrl}/game/join?passcode=${passcode}`, playerId);
    }

    public getPlayersInLobby(gameId: string): Observable<Player[]> {
        return this.http.get<Player[]>(`${this.apiServerUrl}/game/players?id=${gameId}`);
    }

    public setPlayerReady(playerId: string): Observable<string> {
        const headers = new HttpHeaders().set('Content-Type', 'text/plain');
        return this.http.put(`${this.apiServerUrl}/player/ready`, playerId, { headers, responseType: 'text' as 'json' }) as Observable<string>;
    }

    public allPlayersReady(gameId: string): Observable<Boolean> {
        return this.http.get<Boolean>(`${this.apiServerUrl}/game/ready?id=${gameId}`);
    }

    public startGame(): Observable<void> {
        const gameId = this.gameSubject.value?.id;
        return this.http.put<void>(`${this.apiServerUrl}/game/start`, gameId);
    }

    public startClash(): void {
        const gameId = this.gameSubject.value?.id;
        this.http.put<void>(`${this.apiServerUrl}/game/clash/start`, gameId).subscribe({
            next: () => {},
            // error: (e) => console.log(e)
        });
    }

    public clashProcessed(): void {
        const headers = new HttpHeaders().set('Content-Type', 'text/plain');
        const gameId = this.gameSubject.value?.id;
        this.http.put<void>(`${this.apiServerUrl}/game/clash/processed`, gameId, { headers, responseType: 'text' as 'json' }).subscribe({
            next: () => {},
            // error: (e) => console.log(e)
        });
    }

    public rollInitForPlayer(playerId: string): Observable<number> {
        return this.http.get<number>(`${this.apiServerUrl}/player/rollinit?id=${playerId}`)
    }

    public removeCardInPlay(playerId: string): void {
        this.http.delete<void>(`${this.apiServerUrl}/player/cardInPlay?id=${playerId}`).subscribe({
            next: () => {},
            error: (e) => console.log("Error removing card in play: ", e)
        });
    }

    public playerWonClash(playerId: string): void{
        const headers = new HttpHeaders().set('Content-Type', 'text/plain');
        this.http.put<void>(`${this.apiServerUrl}/player/wonClash`, playerId, { headers }).subscribe({
            next: () => {},
            error: (e) => console.log("Error checking if player won: ", e)
        });
    }
}