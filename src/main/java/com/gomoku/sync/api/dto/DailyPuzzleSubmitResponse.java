package com.gomoku.sync.api.dto;

/**
 * POST /api/me/daily-puzzle/submit 结果。
 */
public class DailyPuzzleSubmitResponse {

    /** SOLVED, ALREADY_SOLVED, INVALID, NOT_MET */
    private final String result;
    private final boolean solved;
    private final boolean alreadySolved;
    private final int attemptCount;
    /** 每个自然日第一次通关当日残局时发放的团团积分（activity_points），否则为 0 */
    private final int activityPointsDelta;
    /** 发放后的团团积分总数；未发放时为 null */
    private final Integer activityPointsAfter;

    public DailyPuzzleSubmitResponse(
            String result,
            boolean solved,
            boolean alreadySolved,
            int attemptCount,
            int activityPointsDelta,
            Integer activityPointsAfter) {
        this.result = result;
        this.solved = solved;
        this.alreadySolved = alreadySolved;
        this.attemptCount = attemptCount;
        this.activityPointsDelta = activityPointsDelta;
        this.activityPointsAfter = activityPointsAfter;
    }

    public String getResult() {
        return result;
    }

    public boolean isSolved() {
        return solved;
    }

    public boolean isAlreadySolved() {
        return alreadySolved;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getActivityPointsDelta() {
        return activityPointsDelta;
    }

    public Integer getActivityPointsAfter() {
        return activityPointsAfter;
    }
}
