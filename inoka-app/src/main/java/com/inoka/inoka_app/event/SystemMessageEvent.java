package com.inoka.inoka_app.event;

import org.springframework.context.ApplicationEvent;

import com.inoka.inoka_app.model.ChatType;

public class SystemMessageEvent extends ApplicationEvent{
    private final String gameId;
    private final String content;
    private final ChatType type;

    public SystemMessageEvent(Object source, String gameId, String content, ChatType type) {
        super(source);
        this.gameId = gameId;
        this.content = content;
        this.type = type;
    }

    public String getGameId() {
        return gameId;
    }

    public String getContent() {
        return content;
    }

    public ChatType getType() {
        return type;
    }
}
