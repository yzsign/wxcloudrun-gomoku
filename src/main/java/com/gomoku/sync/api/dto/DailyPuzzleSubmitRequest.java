package com.gomoku.sync.api.dto;

import java.util.List;

/**
 * POST /api/me/daily-puzzle/submit — 从初始局面起的完整手顺（含终局胜手）。
 */
public class DailyPuzzleSubmitRequest {

    private List<GameMoveDto> moves;

    public List<GameMoveDto> getMoves() {
        return moves;
    }

    public void setMoves(List<GameMoveDto> moves) {
        this.moves = moves;
    }
}
