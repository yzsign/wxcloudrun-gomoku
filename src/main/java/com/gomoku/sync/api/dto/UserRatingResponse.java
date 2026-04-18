package com.gomoku.sync.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
    /** 微信 gender：0 未知 1 男 2 女（users.gender；本人与对手接口均可填） */
    private final Integer gender;
    /** 本人 /api/me/rating 专用；对手接口为 null 不输出 */
    private final String checkinLastYmd;
    private final Integer checkinStreak;
    private final List<String> checkinHistory;
    private final Boolean tuanMoeUnlocked;
    /** 本人 /api/me/rating 专用：已解锁的积分/活动棋子皮肤 id 列表 */
    private final List<String> pieceSkinUnlockedIds;
    /** 本人 /api/me/rating 专用：当前佩戴棋子皮肤（与装备槽 PIECE_SKIN 一致） */
    private final String pieceSkinId;
    /** 本人 /api/me/rating 专用：当前界面棋盘主题（装备槽 THEME；未设置时不输出，客户端默认檀木） */
    private final String themeId;
    /**
     * 本人 /api/me/rating 专用：是否装备棋盘技能槽（短剑）；非本人接口传 null 不输出。
     */
    private final Boolean daggerSkillEquipped;
    /** 本人 /api/me/rating 专用：短剑消耗品持有数 */
    private final Integer consumableDaggerCount;
    /**
     * 本人 /api/me/rating 专用：是否装备爱心技能槽；非本人接口传 null 不输出。
     */
    private final Boolean loveSkillEquipped;
    /** 本人 /api/me/rating 专用：爱心消耗品持有数 */
    private final Integer consumableLoveCount;

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
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
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
                nickname,
                avatarUrl,
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
            String avatarUrl,
            Integer gender) {
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
                nickname,
                avatarUrl,
                gender,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
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
            String avatarUrl,
            Integer gender,
            String checkinLastYmd,
            Integer checkinStreak,
            List<String> checkinHistory,
            Boolean tuanMoeUnlocked,
            List<String> pieceSkinUnlockedIds,
            String pieceSkinId,
            String themeId,
            Boolean daggerSkillEquipped,
            Integer consumableDaggerCount,
            Boolean loveSkillEquipped,
            Integer consumableLoveCount) {
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
        this.gender = gender;
        this.checkinLastYmd = checkinLastYmd;
        this.checkinStreak = checkinStreak;
        this.checkinHistory = checkinHistory;
        this.tuanMoeUnlocked = tuanMoeUnlocked;
        this.pieceSkinUnlockedIds = pieceSkinUnlockedIds;
        this.pieceSkinId = pieceSkinId;
        this.themeId = themeId;
        this.daggerSkillEquipped = daggerSkillEquipped;
        this.consumableDaggerCount = consumableDaggerCount;
        this.loveSkillEquipped = loveSkillEquipped;
        this.consumableLoveCount = consumableLoveCount;
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

    public Integer getGender() {
        return gender;
    }

    public String getCheckinLastYmd() {
        return checkinLastYmd;
    }

    public Integer getCheckinStreak() {
        return checkinStreak;
    }

    public List<String> getCheckinHistory() {
        return checkinHistory;
    }

    public Boolean getTuanMoeUnlocked() {
        return tuanMoeUnlocked;
    }

    public List<String> getPieceSkinUnlockedIds() {
        return pieceSkinUnlockedIds;
    }

    public String getPieceSkinId() {
        return pieceSkinId;
    }

    public String getThemeId() {
        return themeId;
    }

    public Boolean getDaggerSkillEquipped() {
        return daggerSkillEquipped;
    }

    public Integer getConsumableDaggerCount() {
        return consumableDaggerCount;
    }

    public Boolean getLoveSkillEquipped() {
        return loveSkillEquipped;
    }

    public Integer getConsumableLoveCount() {
        return consumableLoveCount;
    }
}
