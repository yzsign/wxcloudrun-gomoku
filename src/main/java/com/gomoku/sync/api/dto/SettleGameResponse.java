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
    /**
     * 与 Authorization 中用户对应的天梯/团团分变更（不依赖客户端执子色，避免与 black/white 错配）。
     */
    private final int callerEloAfter;
    private final int callerEloDelta;
    private final int callerActivityPointsAfter;
    private final int callerActivityPointsDelta;
    /** 请求方是否坐在本局黑方座位（与内存房间一致，用于与 black/白 两列对账展示） */
    private final boolean callerPlaysBlack;

    public SettleGameResponse(
            long gameId,
            int blackEloAfter,
            int whiteEloAfter,
            int blackEloDelta,
            int whiteEloDelta,
            int blackActivityPointsAfter,
            int whiteActivityPointsAfter,
            int blackActivityPointsDelta,
            int whiteActivityPointsDelta,
            int callerEloAfter,
            int callerEloDelta,
            int callerActivityPointsAfter,
            int callerActivityPointsDelta,
            boolean callerPlaysBlack) {
        this.gameId = gameId;
        this.blackEloAfter = blackEloAfter;
        this.whiteEloAfter = whiteEloAfter;
        this.blackEloDelta = blackEloDelta;
        this.whiteEloDelta = whiteEloDelta;
        this.blackActivityPointsAfter = blackActivityPointsAfter;
        this.whiteActivityPointsAfter = whiteActivityPointsAfter;
        this.blackActivityPointsDelta = blackActivityPointsDelta;
        this.whiteActivityPointsDelta = whiteActivityPointsDelta;
        this.callerEloAfter = callerEloAfter;
        this.callerEloDelta = callerEloDelta;
        this.callerActivityPointsAfter = callerActivityPointsAfter;
        this.callerActivityPointsDelta = callerActivityPointsDelta;
        this.callerPlaysBlack = callerPlaysBlack;
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

    public int getCallerEloAfter() {
        return callerEloAfter;
    }

    public int getCallerEloDelta() {
        return callerEloDelta;
    }

    public int getCallerActivityPointsAfter() {
        return callerActivityPointsAfter;
    }

    public int getCallerActivityPointsDelta() {
        return callerActivityPointsDelta;
    }

    public boolean isCallerPlaysBlack() {
        return callerPlaysBlack;
    }
}
