package com.gomoku.sync.api.dto;

/**
 * 好友 PVP 对局观战：凭 watchToken 与 sessionToken 连 Gomoku WebSocket，与残局房 spectator 并列。
 */
public class FriendWatchTicketResponse {
    private final String roomId;
    private final String watchToken;
    private final int boardSize;

    public FriendWatchTicketResponse(String roomId, String watchToken, int boardSize) {
        this.roomId = roomId;
        this.watchToken = watchToken;
        this.boardSize = boardSize;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getWatchToken() {
        return watchToken;
    }

    public int getBoardSize() {
        return boardSize;
    }
}
