package com.gomoku.sync.api.dto;

/**
 * GET /api/rooms/{roomId} 公开信息（不含对局 token）
 */
public class RoomResource {

    private final String roomId;
    private final int boardSize;
    private final boolean guestJoined;

    public RoomResource(String roomId, int boardSize, boolean guestJoined) {
        this.roomId = roomId;
        this.boardSize = boardSize;
        this.guestJoined = guestJoined;
    }

    public String getRoomId() {
        return roomId;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public boolean isGuestJoined() {
        return guestJoined;
    }
}
