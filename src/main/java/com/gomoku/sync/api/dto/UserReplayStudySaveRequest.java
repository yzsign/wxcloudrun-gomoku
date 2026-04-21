package com.gomoku.sync.api.dto;

import java.util.List;

/** PUT /api/me/replay-study — 覆盖写入当前用户唯一复盘存档 */
public class UserReplayStudySaveRequest {

    private List<GameMoveDto> moves;
    private int replayStep;
    private int[][] board;
    private int sideToMove;
    private Long sourceGameId;
    private String blackPieceSkinId;
    private String whitePieceSkinId;

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
}
