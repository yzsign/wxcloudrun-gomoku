package com.gomoku.sync.api.dto.admin;

/** PUT /api/admin/daily-puzzle-schedule */
public class AdminDailyPuzzleScheduleRequest {

    /** yyyy-MM-dd */
    private String puzzleDate;
    private long puzzleId;

    public String getPuzzleDate() {
        return puzzleDate;
    }

    public void setPuzzleDate(String puzzleDate) {
        this.puzzleDate = puzzleDate;
    }

    public long getPuzzleId() {
        return puzzleId;
    }

    public void setPuzzleId(long puzzleId) {
        this.puzzleId = puzzleId;
    }
}
