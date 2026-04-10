package com.gomoku.sync.api.dto;

public class SettleGameResponse {

    private final long gameId;
    private final int blackEloAfter;
    private final int whiteEloAfter;
    private final int blackEloDelta;
    private final int whiteEloDelta;
    /** 结算后团团积分（活跃积分） */
    private final int blackActivityPointsAfter;
    private final int whiteActivityPointsAfter;
    /** 本局获得的团团积分，与 {@link com.gomoku.sync.service.RatingSettlementService} 内规则一致 */
    private final int blackActivityPointsDelta;
    private final int whiteActivityPointsDelta;

    public SettleGameResponse(
            long gameId,
            int blackEloAfter,
            int whiteEloAfter,
            int blackEloDelta,
            int whiteEloDelta,
            int blackActivityPointsAfter,
            int whiteActivityPointsAfter,
            int blackActivityPointsDelta,
            int whiteActivityPointsDelta) {
        this.gameId = gameId;
        this.blackEloAfter = blackEloAfter;
        this.whiteEloAfter = whiteEloAfter;
        this.blackEloDelta = blackEloDelta;
        this.whiteEloDelta = whiteEloDelta;
        this.blackActivityPointsAfter = blackActivityPointsAfter;
        this.whiteActivityPointsAfter = whiteActivityPointsAfter;
        this.blackActivityPointsDelta = blackActivityPointsDelta;
        this.whiteActivityPointsDelta = whiteActivityPointsDelta;
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

    public int getBlackActivityPointsAfter() {
        return blackActivityPointsAfter;
    }

    public int getWhiteActivityPointsAfter() {
        return whiteActivityPointsAfter;
    }

    public int getBlackActivityPointsDelta() {
        return blackActivityPointsDelta;
    }

    public int getWhiteActivityPointsDelta() {
        return whiteActivityPointsDelta;
    }
}
