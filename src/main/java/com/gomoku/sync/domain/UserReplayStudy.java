package com.gomoku.sync.domain;

import java.util.Date;

/** 用户对局复盘最新一条存档（表 user_replay_study） */
public class UserReplayStudy {

    private long userId;
    private String movesJson;
    private int replayStep;
    private String boardJson;
    private int sideToMove;
    private Long sourceGameId;
    private String blackPieceSkinId;
    private String whitePieceSkinId;
    private Date updatedAt;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getMovesJson() {
        return movesJson;
    }

    public void setMovesJson(String movesJson) {
        this.movesJson = movesJson;
    }

    public int getReplayStep() {
        return replayStep;
    }

    public void setReplayStep(int replayStep) {
        this.replayStep = replayStep;
    }

    public String getBoardJson() {
        return boardJson;
    }

    public void setBoardJson(String boardJson) {
        this.boardJson = boardJson;
    }

    public int getSideToMove() {
        return sideToMove;
    }

    public void setSideToMove(int sideToMove) {
        this.sideToMove = sideToMove;
    }

    public Long getSourceGameId() {
        return sourceGameId;
    }

    public void setSourceGameId(Long sourceGameId) {
        this.sourceGameId = sourceGameId;
    }

    public String getBlackPieceSkinId() {
        return blackPieceSkinId;
    }

    public void setBlackPieceSkinId(String blackPieceSkinId) {
        this.blackPieceSkinId = blackPieceSkinId;
    }

    public String getWhitePieceSkinId() {
        return whitePieceSkinId;
    }

    public void setWhitePieceSkinId(String whitePieceSkinId) {
        this.whitePieceSkinId = whitePieceSkinId;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
