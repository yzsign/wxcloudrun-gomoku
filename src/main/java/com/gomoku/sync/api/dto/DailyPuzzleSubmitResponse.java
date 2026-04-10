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

    public DailyPuzzleSubmitResponse(String result, boolean solved, boolean alreadySolved, int attemptCount) {
        this.result = result;
        this.solved = solved;
        this.alreadySolved = alreadySolved;
        this.attemptCount = attemptCount;
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
}
