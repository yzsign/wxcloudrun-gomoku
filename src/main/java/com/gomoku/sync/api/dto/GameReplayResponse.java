package com.gomoku.sync.api.dto;

import java.util.Collections;
import java.util.List;

/**
 * GET /api/games/replay 等对局回放数据。
 */
public class GameReplayResponse {

    private long gameId;
    private String roomId;
    private int matchRound;
    private int boardSize;
    private long blackUserId;
    private long whiteUserId;
    private int totalSteps;
    private String outcome;
    private Long runawayUserId;
    private List<GameMoveDto> moves;

    public GameReplayResponse() {}

    public GameReplayResponse(
            long gameId,
            String roomId,
            int matchRound,
            int boardSize,
            long blackUserId,
            long whiteUserId,
            int totalSteps,
            String outcome,
            Long runawayUserId,
            List<GameMoveDto> moves) {
        this.gameId = gameId;
        this.roomId = roomId;
        this.matchRound = matchRound;
        this.boardSize = boardSize;
        this.blackUserId = blackUserId;
        this.whiteUserId = whiteUserId;
        this.totalSteps = totalSteps;
        this.outcome = outcome;
        this.runawayUserId = runawayUserId;
        this.moves = moves != null ? moves : Collections.emptyList();
    }

    public long getGameId() {
        return gameId;
    }

    public void setGameId(long gameId) {
        this.gameId = gameId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public int getMatchRound() {
        return matchRound;
    }

    public void setMatchRound(int matchRound) {
        this.matchRound = matchRound;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(int boardSize) {
        this.boardSize = boardSize;
    }

    public long getBlackUserId() {
        return blackUserId;
    }

    public void setBlackUserId(long blackUserId) {
        this.blackUserId = blackUserId;
    }

    public long getWhiteUserId() {
        return whiteUserId;
    }

    public void setWhiteUserId(long whiteUserId) {
        this.whiteUserId = whiteUserId;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Long getRunawayUserId() {
        return runawayUserId;
    }

    public void setRunawayUserId(Long runawayUserId) {
        this.runawayUserId = runawayUserId;
    }

    public List<GameMoveDto> getMoves() {
        return moves;
    }

    public void setMoves(List<GameMoveDto> moves) {
        this.moves = moves;
    }
}
