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
}
