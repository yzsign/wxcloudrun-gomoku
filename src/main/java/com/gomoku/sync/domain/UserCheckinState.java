package com.gomoku.sync.domain;

import java.util.Date;

/**
 * 表 user_checkin_state：每用户一行签到汇总（与 users 分离）。
 */
public class UserCheckinState {

    private long userId;
    private String lastCheckinYmd;
    private int streak;
    private String historyJson;
    private boolean pieceSkinTuanMoeUnlocked;
    private Date createdAt;
    private Date updatedAt;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getLastCheckinYmd() {
        return lastCheckinYmd;
    }

    public void setLastCheckinYmd(String lastCheckinYmd) {
        this.lastCheckinYmd = lastCheckinYmd;
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }

    public String getHistoryJson() {
        return historyJson;
    }

    public void setHistoryJson(String historyJson) {
        this.historyJson = historyJson;
    }

    public boolean isPieceSkinTuanMoeUnlocked() {
        return pieceSkinTuanMoeUnlocked;
    }

    public void setPieceSkinTuanMoeUnlocked(boolean pieceSkinTuanMoeUnlocked) {
        this.pieceSkinTuanMoeUnlocked = pieceSkinTuanMoeUnlocked;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
