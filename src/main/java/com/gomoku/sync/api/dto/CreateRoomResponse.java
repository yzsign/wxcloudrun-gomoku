package com.gomoku.sync.api.dto;

public class CreateRoomResponse {
    private final String roomId;
    private final String blackToken;
    private final int boardSize;
    private final boolean ranked;

    public CreateRoomResponse(String roomId, String blackToken, int boardSize, boolean ranked) {
        this.roomId = roomId;
        this.blackToken = blackToken;
        this.boardSize = boardSize;
        this.ranked = ranked;
    }

    public CreateRoomResponse(String roomId, String blackToken, int boardSize) {
        this(roomId, blackToken, boardSize, false);
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

    public boolean isRanked() {
        return ranked;
    }
}
