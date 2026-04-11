package com.gomoku.sync.api.dto;

public class PuzzleFriendRoomResponse {

    private final String roomId;
    /** 房主执黑连 WebSocket；好友 POST /join 执白 */
    private final String blackToken;
    /** 仍可用于仅旁观（旧客户端）；新房主应优先用 blackToken 对弈 */
    private final String spectatorToken;
    private final int boardSize;

    public PuzzleFriendRoomResponse(
            String roomId, String blackToken, String spectatorToken, int boardSize) {
        this.roomId = roomId;
        this.blackToken = blackToken;
        this.spectatorToken = spectatorToken;
        this.boardSize = boardSize;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getBlackToken() {
        return blackToken;
    }

    public String getSpectatorToken() {
        return spectatorToken;
    }

    public int getBoardSize() {
        return boardSize;
    }
}
