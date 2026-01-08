import { Injectable } from "@angular/core";
import { Client, IMessage } from "@stomp/stompjs";
import { BehaviorSubject, Observable } from "rxjs";
import { Game, GameView } from "../components/game";
import SockJS from "sockjs-client";
import { Card } from "../components/card";


@Injectable({
    providedIn: 'root'
})
export class GameWebSocketService {
    private stompClient!: Client;
    private gameSubject = new BehaviorSubject<GameView | null>(null);
    private deckSubject = new BehaviorSubject<Card[]>([]);

    public gameUpdates$: Observable<GameView | null> = this.gameSubject.asObservable();
    public deckUpdates$: Observable<Card[] | null> = this.deckSubject.asObservable();

    constructor(){}

    connect(gameId: string): void {
        const socket = new SockJS('http://localhost:8080/ws');
        const token = localStorage.getItem('authToken');
        
        if (!token) {
            console.error('No authentication token found');
            return;
        }
        
        // console.log('Attempting WebSocket connection with token');
        
        this.stompClient = new Client({
            webSocketFactory: () => socket,
            reconnectDelay: 5000,
            connectHeaders: {
                Authorization: `Bearer ${token}`
            },
            debug: (str) => {
                // console.log('STOMP Debug: ' + str);
            }
        });

        this.stompClient.onConnect = (frame) => {
            // console.log('WebSocket connected successfully', frame);
            
            // Subscribe to game updates
            this.stompClient.subscribe(`/topic/game/${gameId}`, (message: IMessage) => {
                const updatedGame: GameView = JSON.parse(message.body);
                // console.log('Parsed game object from websocket:', updatedGame);
                this.gameSubject.next(updatedGame);
            });

            // Subscribe to deck updates
            this.stompClient.subscribe('user/queue/deck', (message: IMessage) => {
                const deck: Card[] = JSON.parse(message.body);
                this.deckSubject.next(deck);
            });
            
            // console.log('Subscribed to /topic/game/' + gameId, subscription);
        }

        this.stompClient.onStompError = (frame) => {
            console.error('WebSocket STOMP error:', frame);
        }

        this.stompClient.onWebSocketError = (error) => {
            console.error('WebSocket connection error:', error);
        }

        this.stompClient.onWebSocketClose = (event) => {
            console.warn('WebSocket connection closed:', event);
        }

        this.stompClient.activate();
    }

    disconnect(): void {
        if (this.stompClient) {
            this.stompClient.deactivate();
        }
    }

    startClash(gameId: string): void {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.publish({
                destination: "/app/clashStart",
                body: gameId
            })
        }
    }

    startNewClash(gameId: string): void {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.publish({
                destination: "/app/clashNew",
                body: gameId
            })
        }
    }

    clashProcessed(gameId: string): void {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.publish({
                destination: "/app/clashProcessed",
                body: gameId
            })
        }
    }

    playCard(card: Card): void {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.publish({
                destination: "/app/playCard",
                body: JSON.stringify(card)
            })
        }
    }

    resolveAction(targetSeat: number): void {
        if (this.stompClient && this.stompClient.connected) {
            const message = { targetSeat };
            this.stompClient.publish({
                destination: "/app/clashAction",
                body: JSON.stringify(message)
            })
        }
    }

    pickedUpKnockout(): void {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.publish({
                destination: "/app/gotKnockout",
                body: ""
            })
        }
    }

    playerForfeitClash(): void {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.publish({
                destination: "/app/clashForfeit",
                body: ""
            })
        }
    }
}