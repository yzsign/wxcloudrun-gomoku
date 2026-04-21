package com.gomoku.sync.api.dto;

import java.util.Collections;
import java.util.List;

/**
 * GET /api/me/replay-study — 当前用户最新复盘存档。
 */
public class UserReplayStudyGetResponse {

    private boolean hasData;
    private int boardSize;
    private List<GameMoveDto> moves;
    private int replayStep;
    private int[][] board;
    private int sideToMove;
    private Long sourceGameId;
    private String blackPieceSkinId;
    private String whitePieceSkinId;
    private Long updatedAtEpochMs;

    public static UserReplayStudyGetResponse empty(int boardSize) {
        UserReplayStudyGetResponse r = new UserReplayStudyGetResponse();
        r.hasData = false;
        r.boardSize = boardSize;
        r.moves = Collections.emptyList();
        r.replayStep = 0;
        r.board = null;
        r.sideToMove = 0;
        return r;
    }

    public boolean isHasData() {
        return hasData;
    }

    public void setHasData(boolean hasData) {
        this.hasData = hasData;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(int boardSize) {
        this.boardSize = boardSize;
    }

    public List<GameMoveDto> getMoves() {
        return moves;
    }

    public void setMoves(List<GameMoveDto> moves) {
        this.moves = moves;
    }

    public int getReplayStep() {
        return replayStep;
    }

    public void setReplayStep(int replayStep) {
        this.replayStep = replayStep;
    }

    public int[][] getBoard() {
        return board;
    }

    public void setBoard(int[][] board) {
        this.board = board;
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

    public Long getUpdatedAtEpochMs() {
        return updatedAtEpochMs;
    }

    public void setUpdatedAtEpochMs(Long updatedAtEpochMs) {
        this.updatedAtEpochMs = updatedAtEpochMs;
    }
}
