package com.gomoku.sync.api.dto.admin;

import com.gomoku.sync.api.dto.GameMoveDto;

import java.util.List;

/** GET /api/admin/daily-puzzles/{id} */
public class AdminDailyPuzzleDetailResponse {

    private final long id;
    private final String title;
    private final int difficulty;
    private final int boardSize;
    private final int[][] board;
    private final int sideToMove;
    private final String goal;
    private final Integer maxUserMoves;
    private final List<GameMoveDto> solutionMoves;
    private final String hintText;
    private final int status;

    public AdminDailyPuzzleDetailResponse(
            long id,
            String title,
            int difficulty,
            int boardSize,
            int[][] board,
            int sideToMove,
            String goal,
            Integer maxUserMoves,
            List<GameMoveDto> solutionMoves,
            String hintText,
            int status) {
        this.id = id;
        this.title = title;
        this.difficulty = difficulty;
        this.boardSize = boardSize;
        this.board = board;
        this.sideToMove = sideToMove;
        this.goal = goal;
        this.maxUserMoves = maxUserMoves;
        this.solutionMoves = solutionMoves;
        this.hintText = hintText;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public int[][] getBoard() {
        return board;
    }

    public int getSideToMove() {
        return sideToMove;
    }

    public String getGoal() {
        return goal;
    }

    public Integer getMaxUserMoves() {
        return maxUserMoves;
    }

    public List<GameMoveDto> getSolutionMoves() {
        return solutionMoves;
    }

    public String getHintText() {
        return hintText;
    }

    public int getStatus() {
        return status;
    }
}
