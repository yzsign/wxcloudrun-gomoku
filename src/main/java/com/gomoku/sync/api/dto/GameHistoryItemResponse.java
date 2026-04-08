package com.gomoku.sync.api.dto;

/**
 * GET /api/me/game-history 单条记录。
 */
public class GameHistoryItemResponse {

    private long gameId;
    private String roomId;
    private int matchRound;
    /** 终局时间（毫秒时间戳，UTC） */
    private long endedAt;
    private String opponentNickname;
    private boolean opponentBot;
    /** 对手头像 URL，可能为空（人机或未设置） */
    private String opponentAvatarUrl;
    /** 对手性别：微信 0 未知 1 男 2 女；可能为 null（未上报） */
    private Integer opponentGender;
    /** 对手 users.id，用于客户端拉取公开战绩；人机为对应人机账号 id */
    private long opponentUserId;
    /** WIN | LOSS | DRAW */
    private String myResult;
    private int totalSteps;

    public GameHistoryItemResponse(
            long gameId,
            String roomId,
            int matchRound,
            long endedAt,
            String opponentNickname,
            boolean opponentBot,
            String opponentAvatarUrl,
            Integer opponentGender,
            long opponentUserId,
            String myResult,
            int totalSteps) {
        this.gameId = gameId;
        this.roomId = roomId;
        this.matchRound = matchRound;
        this.endedAt = endedAt;
        this.opponentNickname = opponentNickname;
        this.opponentBot = opponentBot;
        this.opponentAvatarUrl = opponentAvatarUrl;
        this.opponentGender = opponentGender;
        this.opponentUserId = opponentUserId;
        this.myResult = myResult;
        this.totalSteps = totalSteps;
    }

    public long getGameId() {
        return gameId;
    }

    public String getRoomId() {
        return roomId;
    }

    public int getMatchRound() {
        return matchRound;
    }

    public long getEndedAt() {
        return endedAt;
    }

    public String getOpponentNickname() {
        return opponentNickname;
    }

    public boolean isOpponentBot() {
        return opponentBot;
    }

    public String getOpponentAvatarUrl() {
        return opponentAvatarUrl;
    }

    public Integer getOpponentGender() {
        return opponentGender;
    }

    public long getOpponentUserId() {
        return opponentUserId;
    }

    public String getMyResult() {
        return myResult;
    }

    public int getTotalSteps() {
        return totalSteps;
    }
}
