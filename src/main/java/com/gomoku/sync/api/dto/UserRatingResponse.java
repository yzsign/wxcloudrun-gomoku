package com.gomoku.sync.api.dto;

public class UserRatingResponse {

    private final long userId;
    private final int eloScore;
    private final int activityPoints;
    private final int consecutiveWins;
    private final int consecutiveLosses;
    private final int totalGames;
    private final int winCount;
    private final int drawCount;
    private final int runawayCount;
    private final boolean lowTrust;
    private final int placementFairGames;
    private final int newbieMatchGames;
    /** 可选：昵称（对手接口或本人接口均可填） */
    private final String nickname;
    /** 可选：头像 URL（与 users.avatar_url 一致，需下载域名白名单） */
    private final String avatarUrl;

    public UserRatingResponse(
            long userId,
            int eloScore,
            int activityPoints,
            int consecutiveWins,
            int consecutiveLosses,
            int totalGames,
            int winCount,
            int drawCount,
            int runawayCount,
            boolean lowTrust,
            int placementFairGames,
            int newbieMatchGames) {
        this(
                userId,
                eloScore,
                activityPoints,
                consecutiveWins,
                consecutiveLosses,
                totalGames,
                winCount,
                drawCount,
                runawayCount,
                lowTrust,
                placementFairGames,
                newbieMatchGames,
                null,
                null);
    }

    public UserRatingResponse(
            long userId,
            int eloScore,
            int activityPoints,
            int consecutiveWins,
            int consecutiveLosses,
            int totalGames,
            int winCount,
            int drawCount,
            int runawayCount,
            boolean lowTrust,
            int placementFairGames,
            int newbieMatchGames,
            String nickname,
            String avatarUrl) {
        this.userId = userId;
        this.eloScore = eloScore;
        this.activityPoints = activityPoints;
        this.consecutiveWins = consecutiveWins;
        this.consecutiveLosses = consecutiveLosses;
        this.totalGames = totalGames;
        this.winCount = winCount;
        this.drawCount = drawCount;
        this.runawayCount = runawayCount;
        this.lowTrust = lowTrust;
        this.placementFairGames = placementFairGames;
        this.newbieMatchGames = newbieMatchGames;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }

    public long getUserId() {
        return userId;
    }

    public int getEloScore() {
        return eloScore;
    }

    public int getActivityPoints() {
        return activityPoints;
    }

    public int getConsecutiveWins() {
        return consecutiveWins;
    }

    public int getConsecutiveLosses() {
        return consecutiveLosses;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public int getWinCount() {
        return winCount;
    }

    public int getDrawCount() {
        return drawCount;
    }

    public int getRunawayCount() {
        return runawayCount;
    }

    public boolean isLowTrust() {
        return lowTrust;
    }

    public int getPlacementFairGames() {
        return placementFairGames;
    }

    public int getNewbieMatchGames() {
        return newbieMatchGames;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
