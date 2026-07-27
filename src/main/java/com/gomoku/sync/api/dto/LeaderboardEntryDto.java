package com.gomoku.sync.api.dto;

/**
 * 天梯榜单行（rule.md §8.2 排序后的前 N 名）
 */
public class LeaderboardEntryDto {

    private final int rank;
    private final long userId;
    private final String nickname;
    private final String avatarUrl;
    private final int eloScore;
    private final String titleName;
    private final int totalGames;
    private final int winCount;

    public LeaderboardEntryDto(
            int rank,
            long userId,
            String nickname,
            String avatarUrl,
            int eloScore,
            String titleName,
            int totalGames,
            int winCount) {
        this.rank = rank;
        this.userId = userId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.eloScore = eloScore;
        this.titleName = titleName;
        this.totalGames = totalGames;
        this.winCount = winCount;
    }

    public int getRank() {
        return rank;
    }

    public long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public int getEloScore() {
        return eloScore;
    }

    public String getTitleName() {
        return titleName;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public int getWinCount() {
        return winCount;
    }
}
