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
        this.stompClient = new Client({
            webSocketFactory: () => socket,
            reconnectDelay: 5000
        });

        this.stompClient.onConnect = () => {
            // console.log('Connected to WebSocket for gameId:', gameId);
            this.stompClient.subscribe(`/topic/game/${gameId}`, (message: IMessage) => {
                const updatedGame: Game = JSON.parse(message.body);
                // console.log('Received game update: ', updatedGame);
                this.gameSubject.next(updatedGame);
            });
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