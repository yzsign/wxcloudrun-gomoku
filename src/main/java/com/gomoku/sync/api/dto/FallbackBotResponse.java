package com.gomoku.sync.api.dto;

import com.gomoku.sync.domain.User;

/**
 * 随机匹配超时后接入数据库人机成功；并返回入座人机的公开资料（与小游戏 applyOnlineOpponentProfilePayload 字段一致）。
 */
public class FallbackBotResponse {

    private final boolean paired;
    private final int boardSize;
    /** 入座白方人机 users.id */
    private final Long userId;
    private final String nickname;
    private final String avatarUrl;
    /** 微信 userInfo.gender：0 未知 1 男 2 女 */
    private final Integer gender;

    public FallbackBotResponse(boolean paired, int boardSize) {
        this(paired, boardSize, null, null, null, null);
    }

    public FallbackBotResponse(boolean paired, int boardSize, User bot) {
        this(
                paired,
                boardSize,
                bot != null ? bot.getId() : null,
                bot != null ? bot.getNickname() : null,
                bot != null ? bot.getAvatarUrl() : null,
                bot != null ? bot.getGender() : null);
    }

    public FallbackBotResponse(
            boolean paired,
            int boardSize,
            Long userId,
            String nickname,
            String avatarUrl,
            Integer gender) {
        this.paired = paired;
        this.boardSize = boardSize;
        this.userId = userId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.gender = gender;
    }

    public boolean isPaired() {
        return paired;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public Long getUserId() {
        return userId;
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
}
