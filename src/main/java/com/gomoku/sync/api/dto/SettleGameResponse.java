package com.gomoku.sync.api.dto;

public class SettleGameResponse {

    private final long gameId;
    private final int blackEloAfter;
    private final int whiteEloAfter;
    private final int blackEloDelta;
    private final int whiteEloDelta;

    public SettleGameResponse(
            long gameId,
            int blackEloAfter,
            int whiteEloAfter,
            int blackEloDelta,
            int whiteEloDelta) {
        this.gameId = gameId;
        this.blackEloAfter = blackEloAfter;
        this.whiteEloAfter = whiteEloAfter;
        this.blackEloDelta = blackEloDelta;
        this.whiteEloDelta = whiteEloDelta;
    }

    public long getGameId() {
        return gameId;
    }

    public int getBlackEloAfter() {
        return blackEloAfter;
    }

    public int getWhiteEloAfter() {
        return whiteEloAfter;
    }

    public int getBlackEloDelta() {
        return blackEloDelta;
    }

    public int getWhiteEloDelta() {
        return whiteEloDelta;
    }
}
