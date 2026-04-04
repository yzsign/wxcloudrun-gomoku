package com.gomoku.sync.domain;

import java.time.LocalDate;
import java.util.Date;

/**
 * 与表 users 对应（含 rule.md 天梯字段）
 */
public class User {

    private Long id;
    private String openid;
    private boolean bot;
    private String unionid;
    private String nickname;
    private String avatarUrl;
    private int eloScore = 1200;
    private int activityPoints;
    private int consecutiveWins;
    private int consecutiveLosses;
    private int todayNetChange;
    private int eloCarryOver;
    private LocalDate lastRatingResetDate;
    private int runawayCount;
    private int totalGames;
    private int winCount;
    private int drawCount;
    private Integer seasonEndScore;
    private int placementFairGames;
    private int newbieMatchGames;
    private int newbieRunawayTally;
    private boolean lowTrust;
    private Date lastLoginAt;
    private Date createdAt;
    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public boolean isBot() {
        return bot;
    }

    public void setBot(boolean bot) {
        this.bot = bot;
    }

    public String getUnionid() {
        return unionid;
    }

    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public int getEloScore() {
        return eloScore;
    }

    public void setEloScore(int eloScore) {
        this.eloScore = eloScore;
    }

    public int getActivityPoints() {
        return activityPoints;
    }

    public void setActivityPoints(int activityPoints) {
        this.activityPoints = activityPoints;
    }

    public int getConsecutiveWins() {
        return consecutiveWins;
    }

    public void setConsecutiveWins(int consecutiveWins) {
        this.consecutiveWins = consecutiveWins;
    }

    public int getConsecutiveLosses() {
        return consecutiveLosses;
    }

    public void setConsecutiveLosses(int consecutiveLosses) {
        this.consecutiveLosses = consecutiveLosses;
    }

    public int getTodayNetChange() {
        return todayNetChange;
    }

    public void setTodayNetChange(int todayNetChange) {
        this.todayNetChange = todayNetChange;
    }

    public int getEloCarryOver() {
        return eloCarryOver;
    }

    public void setEloCarryOver(int eloCarryOver) {
        this.eloCarryOver = eloCarryOver;
    }

    public LocalDate getLastRatingResetDate() {
        return lastRatingResetDate;
    }

    public void setLastRatingResetDate(LocalDate lastRatingResetDate) {
        this.lastRatingResetDate = lastRatingResetDate;
    }

    public int getRunawayCount() {
        return runawayCount;
    }

    public void setRunawayCount(int runawayCount) {
        this.runawayCount = runawayCount;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(int totalGames) {
        this.totalGames = totalGames;
    }

    public int getWinCount() {
        return winCount;
    }

    public void setWinCount(int winCount) {
        this.winCount = winCount;
    }

    public int getDrawCount() {
        return drawCount;
    }

    public void setDrawCount(int drawCount) {
        this.drawCount = drawCount;
    }

    public Integer getSeasonEndScore() {
        return seasonEndScore;
    }

    public void setSeasonEndScore(Integer seasonEndScore) {
        this.seasonEndScore = seasonEndScore;
    }

    public int getPlacementFairGames() {
        return placementFairGames;
    }

    public void setPlacementFairGames(int placementFairGames) {
        this.placementFairGames = placementFairGames;
    }

    public int getNewbieMatchGames() {
        return newbieMatchGames;
    }

    public void setNewbieMatchGames(int newbieMatchGames) {
        this.newbieMatchGames = newbieMatchGames;
    }

    public int getNewbieRunawayTally() {
        return newbieRunawayTally;
    }

    public void setNewbieRunawayTally(int newbieRunawayTally) {
        this.newbieRunawayTally = newbieRunawayTally;
    }

    public boolean isLowTrust() {
        return lowTrust;
    }

    public void setLowTrust(boolean lowTrust) {
        this.lowTrust = lowTrust;
    }

    public Date getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Date lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
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
