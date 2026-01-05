import { Injectable } from "@angular/core";
import { Client, IMessage } from "@stomp/stompjs";
import { BehaviorSubject, Observable } from "rxjs";
import { Game } from "../components/game";
import SockJS from "sockjs-client";
import { Card } from "../components/card";


@Injectable({
    providedIn: 'root'
})
export class GameWebSocketService {
    private stompClient!: Client;
    private gameSubject = new BehaviorSubject<Game | null>(null);

    public gameUpdates$: Observable<Game | null> = this.gameSubject.asObservable();

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
            // console.log('✅ WebSocket connected successfully', frame);
            
            // Subscribe to game updates
            const subscription = this.stompClient.subscribe(`/topic/game/${gameId}`, (message: IMessage) => {
                // console.log('📨 Received game update from WebSocket:', message.body);
                const updatedGame: Game = JSON.parse(message.body);
                // console.log('🎮 Parsed game object:', updatedGame);
                this.gameSubject.next(updatedGame);
            });
            
            // console.log('✅ Subscribed to /topic/game/' + gameId, subscription);
        }

        this.stompClient.onStompError = (frame) => {
            // console.error('❌ WebSocket STOMP error:', frame);
            // console.error('Error headers:', frame.headers);
            // console.error('Error body:', frame.body);
        }

        this.stompClient.onWebSocketError = (error) => {
            // console.error('❌ WebSocket connection error:', error);
        }

        this.stompClient.onWebSocketClose = (event) => {
            // console.warn('⚠️ WebSocket connection closed:', event);
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

    playCard(playerId: string, card: Card): void {
        if (this.stompClient && this.stompClient.connected) {
            const message = { playerId, card };
            this.stompClient.publish({
                destination: "/app/playCard",
                body: JSON.stringify(message)
            })
        }
    }

    resolveAction(userId: string, targetId: string): void {
        if (this.stompClient && this.stompClient.connected) {
            const message = { userId, targetId };
            this.stompClient.publish({
                destination: "/app/clashAction",
                body: JSON.stringify(message)
            })
        }
    }

    pickedUpKnockout(playerId: string): void {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.publish({
                destination: "/app/gotKnockout",
                body: playerId
            })
        }
    }

    playerForfeitClash(playerId: string): void {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.publish({
                destination: "/app/clashForfeit",
                body: playerId
            })
        }
    }
}