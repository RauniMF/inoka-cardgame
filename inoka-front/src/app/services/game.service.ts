import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { BehaviorSubject, Observable } from "rxjs"
import { Player } from "../components/player";
import { Card } from "../components/card";
import { Game, GameView } from "../components/game";
import { GameWebSocketService } from "./game-websocket.service";

@Injectable({
    providedIn: 'root' // Ensures a singleton instance across the entire application
})
export class GameService {
    private apiServerUrl = 'http://localhost:8080/inoka';
    private currentGameId: string | null = null;

    // BehaviorSubject holds player's data
    private playerSubject = new BehaviorSubject<Player | null>(null);
    public player$ = this.playerSubject.asObservable();

    constructor(private http: HttpClient, private gameWebSocketService: GameWebSocketService) {
        // Initialize player data when service starts
        this.loadPlayer();
    }

    /*
     * Player object methods
    */
    private loadPlayer(): void {
        const storedUUID = localStorage.getItem('userUUID');
        const storedToken = localStorage.getItem('authToken');
        
        if (storedUUID && storedToken) {
            // Find player with ID in back-end (token will be sent via interceptor)
            this.findPlayer()
            .subscribe({
                next: (p) => {
                    this.playerSubject.next(p);
                    console.log("Player loaded from existing session");
                    this.handleWS(p);
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
                    this.handleWS(response.player);
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

    // Handle WebSocket connection from player updates
    private handleWS(player: Player): void {
        if (player.gameId && player.gameId !== 'Not in game') {
            // Player in game = establish connection
            if (this.currentGameId !== player.gameId) {
                this.currentGameId = player.gameId;
                this.gameWebSocketService.connect(player.gameId);
                // INFO:
                console.log("WebSocket connection opened.");
            }
        }
        else {
            // Player not in game = disconnect
            if (this.currentGameId !== null) {
                this.gameWebSocketService.disconnect();
                this.currentGameId = null;
                // INFO:
                console.log("WebSocket disconnected.")
            }
        }
    }

    /** @deprecated Endpoint disabled */
    public getAllPlayers(): Observable<Player[]> {
        return this.http.get<Player[]>(`${this.apiServerUrl}/player/all`);
    }

    // Player lookup via principal
    public findPlayer(): Observable<Player> {
        return this.http.get<Player>(`${this.apiServerUrl}/player/find`);
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
            this.http.put<void>(
                `${this.apiServerUrl}/player/update?name=${encodeURIComponent(name)}`,
                null
            ).subscribe({
                next: (r) => {
                    currentPlayer.name = name;
                    this.playerSubject.next(currentPlayer);
                    // INFO:
                    //console.log('PUT request success: ', r);
                },
                error: (e) => console.error("Failed to update player: ", e)
            });
        }
    }

    // Player joins game and updates BehaviorSubject
    public createGame(passcode: string = ""): void {
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
                    // INFO:
                    // console.log('Joined game with id=', gameId);
                    this.handleWS(currentPlayer);
                },
                error: (e) => console.error("Failed to create game: ", e)
            });
        }
    }

    // Receive existing game object
    public getGame(): Observable<GameView> {
        return this.http.get<GameView>(`${this.apiServerUrl}/game/find`);
    }

    public removePlayer(): Observable<void> {
        return this.http.delete<void>(`${this.apiServerUrl}/player/remove`);
    }

    /** @deprecated Endpoint disabled */
    public removeAllPlayers(): Observable<void> {
        return this.http.delete<void>(`${this.apiServerUrl}/player/remove/all`);
    }

    public getPlayerDeck(): Observable<Card[]> {
        return this.http.get<Card[]>(`${this.apiServerUrl}/player/card/all`);
    }

    public getPlayersInLobby(gameId: string): Observable<Player[]> {
        return this.http.get<Player[]>(`${this.apiServerUrl}/game/players?id=${gameId}`);
    }

    public setPlayerReady(): Observable<string> {
        return this.http.put<string>(`${this.apiServerUrl}/player/ready`, null);
    }

    public allPlayersReady(gameId: string): Observable<Boolean> {
        return this.http.get<Boolean>(`${this.apiServerUrl}/game/ready?id=${gameId}`);
    }

    public startGame(gameId: string): Observable<void> {
        return this.http.put<void>(`${this.apiServerUrl}/game/start`, gameId);
    }

    /** @ignore Clash start handled by websocket service */
    public startClash(gameId: string): void {
        this.http.put<void>(`${this.apiServerUrl}/game/clash/start`, gameId).subscribe({
            next: () => {},
            // error: (e) => console.log(e)
        });
    }

    /** @ignore Clash processing handled by websocket service */
    public clashProcessed(gameId: string): void {
        const headers = new HttpHeaders().set('Content-Type', 'text/plain');
        this.http.put<void>(`${this.apiServerUrl}/game/clash/processed`, gameId, { headers, responseType: 'text' as 'json' }).subscribe({
            next: () => {},
            // error: (e) => console.log(e)
        });
    }

    public rollInitForPlayer(): Observable<number> {
        return this.http.get<number>(`${this.apiServerUrl}/player/rollinit`)
    }

    public removeCardInPlay(): void {
        this.http.delete<void>(`${this.apiServerUrl}/player/cardInPlay`).subscribe({
            next: () => {},
            error: (e) => console.log("Error removing card in play: ", e)
        });
    }

    public playerWonClash(): void{
        this.http.put<void>(`${this.apiServerUrl}/player/wonClash`, null).subscribe({
            next: () => {},
            error: (e) => console.log("Error checking if player won: ", e)
        });
    }

    public getPlayerSeat(): Observable<number> {
        return this.http.get<number>(`${this.apiServerUrl}/player/seat`);
    }
}