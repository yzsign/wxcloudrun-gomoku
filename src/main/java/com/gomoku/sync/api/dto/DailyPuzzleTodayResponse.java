package com.gomoku.sync.api.dto;

/**
 * GET /api/me/daily-puzzle/today
 */
public class DailyPuzzleTodayResponse {

    private final boolean scheduled;
    /** yyyy-MM-dd */
    private final String puzzleDate;
    private final Long puzzleId;
    private final String title;
    private final int difficulty;
    private final int boardSize;
    /** 初始棋盘，与线上一致：0 空 1 黑 2 白 */
    private final int[][] board;
    /** 下一手：1 黑 2 白 */
    private final int sideToMove;
    /** WIN / DRAW */
    private final String goal;
    private final Integer maxUserMoves;
    private final String status;
    private final int attemptCount;
    private final boolean hintUsed;
    /** 仅当 hintUsed 为 true 或题目无提示时：无提示则为 null */
    private final String hintText;
    private final boolean hasHint;

    public DailyPuzzleTodayResponse(
            boolean scheduled,
            String puzzleDate,
            Long puzzleId,
            String title,
            int difficulty,
            int boardSize,
            int[][] board,
            int sideToMove,
            String goal,
            Integer maxUserMoves,
            String status,
            int attemptCount,
            boolean hintUsed,
            String hintText,
            boolean hasHint) {
        this.scheduled = scheduled;
        this.puzzleDate = puzzleDate;
        this.puzzleId = puzzleId;
        this.title = title;
        this.difficulty = difficulty;
        this.boardSize = boardSize;
        this.board = board;
        this.sideToMove = sideToMove;
        this.goal = goal;
        this.maxUserMoves = maxUserMoves;
        this.status = status;
        this.attemptCount = attemptCount;
        this.hintUsed = hintUsed;
        this.hintText = hintText;
        this.hasHint = hasHint;
    }

    /** 今日未排期 */
    public static DailyPuzzleTodayResponse notScheduled(String puzzleDate) {
        return new DailyPuzzleTodayResponse(
                false,
                puzzleDate,
                null,
                null,
                0,
                0,
                null,
                0,
                null,
                null,
                null,
                0,
                false,
                null,
                false);
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public String getPuzzleDate() {
        return puzzleDate;
    }

    public Long getPuzzleId() {
        return puzzleId;
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

    public String getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public boolean isHintUsed() {
        return hintUsed;
    }

    public String getHintText() {
        return hintText;
    }

    public boolean isHasHint() {
        return hasHint;
    }
}
