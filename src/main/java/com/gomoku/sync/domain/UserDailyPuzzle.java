package com.gomoku.sync.domain;

import java.util.Date;

/**
 * 用户每日残局进度 user_daily_puzzle
 */
public class UserDailyPuzzle {

    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_SOLVED = "SOLVED";

    private long userId;
    /** yyyy-MM-dd */
    private String puzzleDate;
    private long puzzleId;
    private String status;
    private int attemptCount;
    private Integer bestMovesToWin;
    private String lastAttemptMovesJson;
    private Date solvedAt;
    private boolean hintUsed;
    private Date updatedAt;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Integer getBestMovesToWin() {
        return bestMovesToWin;
    }

    public void setBestMovesToWin(Integer bestMovesToWin) {
        this.bestMovesToWin = bestMovesToWin;
    }

    public String getLastAttemptMovesJson() {
        return lastAttemptMovesJson;
    }

    public void setLastAttemptMovesJson(String lastAttemptMovesJson) {
        this.lastAttemptMovesJson = lastAttemptMovesJson;
    }

    public Date getSolvedAt() {
        return solvedAt;
    }

    public void setSolvedAt(Date solvedAt) {
        this.solvedAt = solvedAt;
    }

    public boolean isHintUsed() {
        return hintUsed;
    }

    public void setHintUsed(boolean hintUsed) {
        this.hintUsed = hintUsed;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
