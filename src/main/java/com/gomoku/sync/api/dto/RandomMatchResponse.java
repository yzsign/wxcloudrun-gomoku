package com.gomoku.sync.api.dto;

/**
 * 随机匹配：host 等待白方；guest 与队列中房间配对成功
 */
public class RandomMatchResponse {

    /** "host" | "guest" */
    private final String role;
    private final String roomId;
    private final String blackToken;
    private final String whiteToken;
    private final int boardSize;

    public RandomMatchResponse(String role, String roomId, String blackToken, String whiteToken, int boardSize) {
        this.role = role;
        this.roomId = roomId;
        this.blackToken = blackToken;
        this.whiteToken = whiteToken;
        this.boardSize = boardSize;
    }

    public String getRole() {
        return role;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getBlackToken() {
        return blackToken;
    }

    public String getWhiteToken() {
        return whiteToken;
    }

    public int getBoardSize() {
        return boardSize;
    }
}
