package com.gomoku.sync.api.dto;

public class SettleGameResponse {

    private final int blackEloAfter;
    private final int whiteEloAfter;
    private final int blackEloDelta;
    private final int whiteEloDelta;

    public SettleGameResponse(int blackEloAfter, int whiteEloAfter, int blackEloDelta, int whiteEloDelta) {
        this.blackEloAfter = blackEloAfter;
        this.whiteEloAfter = whiteEloAfter;
        this.blackEloDelta = blackEloDelta;
        this.whiteEloDelta = whiteEloDelta;
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
