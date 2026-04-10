package com.gomoku.sync.domain;

import com.gomoku.sync.api.dto.GameMoveDto;

import java.util.List;

/**
 * 从初始局面重放手顺并判题（与线上一致：五连即胜，无禁手）。
 */
public final class DailyPuzzleReplay {

    public enum Result {
        /** 达成题目要求 */
        SOLVED,
        /** 手顺非法或超步数 */
        INVALID,
        /** 终局未达成要求（错胜方、未下完等） */
        NOT_MET
    }

    private DailyPuzzleReplay() {}

    public static Result evaluate(
            int[][] initialBoard,
            int boardSize,
            int sideToMove,
            String goal,
            Integer maxTotalMoves,
            List<GameMoveDto> moves) {

        if (initialBoard == null || boardSize < 5) {
            return Result.INVALID;
        }
        if (sideToMove != Stone.BLACK && sideToMove != Stone.WHITE) {
            return Result.INVALID;
        }
        if (moves == null || moves.isEmpty()) {
            return Result.NOT_MET;
        }
        if (maxTotalMoves != null && moves.size() > maxTotalMoves) {
            return Result.INVALID;
        }
        if (!boardDimensionsOk(initialBoard, boardSize)) {
            return Result.INVALID;
        }

        int[][] board = copyBoard(initialBoard, boardSize);
        int current = sideToMove;
        int firstPlayer = sideToMove;
        String g = goal != null ? goal.trim().toUpperCase() : DailyPuzzle.GOAL_WIN;

        for (GameMoveDto m : moves) {
            if (m == null) {
                return Result.INVALID;
            }
            if (m.getColor() != current) {
                return Result.INVALID;
            }
            if (m.getColor() != Stone.BLACK && m.getColor() != Stone.WHITE) {
                return Result.INVALID;
            }
            int r = m.getR();
            int c = m.getC();
            if (r < 0 || r >= boardSize || c < 0 || c >= boardSize) {
                return Result.INVALID;
            }
            if (board[r][c] != Stone.EMPTY) {
                return Result.INVALID;
            }
            board[r][c] = m.getColor();
            if (WinChecker.checkWin(board, boardSize, r, c, m.getColor())) {
                return evaluateTerminal(g, firstPlayer, m.getColor(), true, false);
            }
            current = opposite(m.getColor());
        }

        if (WinChecker.boardFull(board, boardSize)) {
            return evaluateTerminal(g, firstPlayer, null, false, true);
        }
        return Result.NOT_MET;
    }

    private static Result evaluateTerminal(
            String goal,
            int firstPlayer,
            Integer winnerColor,
            boolean wonByFive,
            boolean boardFullNoWin) {

        if (DailyPuzzle.GOAL_DRAW.equals(goal)) {
            if (wonByFive) {
                return Result.NOT_MET;
            }
            if (boardFullNoWin) {
                return Result.SOLVED;
            }
            return Result.NOT_MET;
        }
        // WIN：先手方走出五连胜
        if (wonByFive && winnerColor != null && winnerColor == firstPlayer) {
            return Result.SOLVED;
        }
        return Result.NOT_MET;
    }

    private static boolean boardDimensionsOk(int[][] board, int size) {
        if (board.length != size) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (board[i] == null || board[i].length != size) {
                return false;
            }
        }
        return true;
    }

    private static int[][] copyBoard(int[][] src, int size) {
        int[][] b = new int[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(src[i], 0, b[i], 0, size);
        }
        return b;
    }

    private static int opposite(int color) {
        return color == Stone.BLACK ? Stone.WHITE : Stone.BLACK;
    }
}
