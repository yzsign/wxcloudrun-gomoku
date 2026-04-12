package com.gomoku.sync.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomChatMessageItem {

    private final long id;
    private final String roomId;
    private final long senderUserId;
    private final int senderColor;
    private final String kind;
    private final String text;
    private final long createdAt;

    public RoomChatMessageItem(
            long id,
            String roomId,
            long senderUserId,
            int senderColor,
            String kind,
            String text,
            long createdAt) {
        this.id = id;
        this.roomId = roomId;
        this.senderUserId = senderUserId;
        this.senderColor = senderColor;
        this.kind = kind;
        this.text = text;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public long getSenderUserId() {
        return senderUserId;
    }

    public int getSenderColor() {
        return senderColor;
    }

    public String getKind() {
        return kind;
    }

    public String getText() {
        return text;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
