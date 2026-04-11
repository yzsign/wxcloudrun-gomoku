package com.gomoku.sync.api.dto;

public class PuzzleFriendRoomResponse {

    private final String roomId;
    private final String spectatorToken;
    private final int boardSize;

    public PuzzleFriendRoomResponse(String roomId, String spectatorToken, int boardSize) {
        this.roomId = roomId;
        this.spectatorToken = spectatorToken;
        this.boardSize = boardSize;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getSpectatorToken() {
        return spectatorToken;
    }

    public int getBoardSize() {
        return boardSize;
    }
}
