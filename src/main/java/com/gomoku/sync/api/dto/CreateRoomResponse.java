package com.gomoku.sync.api.dto;

public class CreateRoomResponse {
    private final String roomId;
    private final String blackToken;
    private final int boardSize;

    public CreateRoomResponse(String roomId, String blackToken, int boardSize) {
        this.roomId = roomId;
        this.blackToken = blackToken;
        this.boardSize = boardSize;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getBlackToken() {
        return blackToken;
    }

    public int getBoardSize() {
        return boardSize;
    }
}
