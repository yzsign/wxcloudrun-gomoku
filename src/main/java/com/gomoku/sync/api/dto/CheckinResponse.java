package com.gomoku.sync.api.dto;

import java.util.Collections;
import java.util.List;

/**
 * POST /api/me/checkin 响应：签到成功、已签、或失败说明。
 */
public class CheckinResponse {

    private final boolean ok;
    private final boolean alreadySigned;
    private final int streak;
    private final int rewardPoints;
    private final int totalPoints;
    private final boolean tuanMoeUnlocked;
    private final boolean newlyUnlockedTuanMoe;
    private final String checkinLastYmd;
    private final List<String> checkinHistory;
    /** 补签消耗积分；普通签到为 0 */
    private final int pointsSpent;
    private final boolean makeup;

    public CheckinResponse(
            boolean ok,
            boolean alreadySigned,
            int streak,
            int rewardPoints,
            int totalPoints,
            boolean tuanMoeUnlocked,
            boolean newlyUnlockedTuanMoe,
            String checkinLastYmd,
            List<String> checkinHistory) {
        this(
                ok,
                alreadySigned,
                streak,
                rewardPoints,
                totalPoints,
                tuanMoeUnlocked,
                newlyUnlockedTuanMoe,
                checkinLastYmd,
                checkinHistory,
                0,
                false);
    }

    public CheckinResponse(
            boolean ok,
            boolean alreadySigned,
            int streak,
            int rewardPoints,
            int totalPoints,
            boolean tuanMoeUnlocked,
            boolean newlyUnlockedTuanMoe,
            String checkinLastYmd,
            List<String> checkinHistory,
            int pointsSpent,
            boolean makeup) {
        this.ok = ok;
        this.alreadySigned = alreadySigned;
        this.streak = streak;
        this.rewardPoints = rewardPoints;
        this.totalPoints = totalPoints;
        this.tuanMoeUnlocked = tuanMoeUnlocked;
        this.newlyUnlockedTuanMoe = newlyUnlockedTuanMoe;
        this.checkinLastYmd = checkinLastYmd;
        this.checkinHistory = checkinHistory != null ? checkinHistory : Collections.emptyList();
        this.pointsSpent = Math.max(0, pointsSpent);
        this.makeup = makeup;
    }

    public boolean isOk() {
        return ok;
    }

    public boolean isAlreadySigned() {
        return alreadySigned;
    }

    public int getStreak() {
        return streak;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public boolean isTuanMoeUnlocked() {
        return tuanMoeUnlocked;
    }

    public boolean isNewlyUnlockedTuanMoe() {
        return newlyUnlockedTuanMoe;
    }

    public String getCheckinLastYmd() {
        return checkinLastYmd;
    }

    public List<String> getCheckinHistory() {
        return checkinHistory;
    }

    public int getPointsSpent() {
        return pointsSpent;
    }

    public boolean isMakeup() {
        return makeup;
    }
}
