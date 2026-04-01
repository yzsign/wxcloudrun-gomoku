package com.gomoku.sync.api.dto;

public class JoinRoomResponse {
    private final String whiteToken;
    private final int boardSize;

    public JoinRoomResponse(String whiteToken, int boardSize) {
        this.whiteToken = whiteToken;
        this.boardSize = boardSize;
    }

    public String getWhiteToken() {
        return whiteToken;
    }

    public int getBoardSize() {
        return boardSize;
    }
}
