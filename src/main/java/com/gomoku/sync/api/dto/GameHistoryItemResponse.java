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
            String myResult,
            int totalSteps) {
        this.gameId = gameId;
        this.roomId = roomId;
        this.matchRound = matchRound;
        this.endedAt = endedAt;
        this.opponentNickname = opponentNickname;
        this.opponentBot = opponentBot;
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

    public String getMyResult() {
        return myResult;
    }

    public int getTotalSteps() {
        return totalSteps;
    }
}
