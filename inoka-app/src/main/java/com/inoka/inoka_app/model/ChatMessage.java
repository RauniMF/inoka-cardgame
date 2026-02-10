package com.inoka.inoka_app.model;

import java.time.Instant;

public class ChatMessage {
    private final PlayerView sender;
    private final String content;
    private final Instant timestamp;
    private final ChatType type;
    
    public ChatMessage(PlayerView sender, String content, Instant timestamp, ChatType type) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.type = type;
    }

    public PlayerView getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public ChatType getType() {
        return type;
    }
}
