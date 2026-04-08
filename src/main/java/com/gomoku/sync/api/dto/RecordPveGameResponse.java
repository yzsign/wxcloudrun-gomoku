package com.gomoku.sync.api.dto;

public class RecordPveGameResponse {

    private final long gameId;

    public RecordPveGameResponse(long gameId) {
        this.gameId = gameId;
    }

    public long getGameId() {
        return gameId;
    }
}
