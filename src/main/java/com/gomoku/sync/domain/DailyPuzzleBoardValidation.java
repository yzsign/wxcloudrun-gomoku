package com.gomoku.sync.domain;

/**
 * 残局 / 每日题棋盘格校验（与管理端、房间逻辑共用）。
 */
public final class DailyPuzzleBoardValidation {

    private DailyPuzzleBoardValidation() {
    }

    public static void validateBoardCells(int[][] board, int size) {
        if (board == null) {
            throw new IllegalArgumentException("board 不能为空");
        }
        if (board.length != size) {
            throw new IllegalArgumentException("board 行数须等于 boardSize");
        }
        for (int r = 0; r < size; r++) {
            if (board[r] == null || board[r].length != size) {
                throw new IllegalArgumentException("board 每行列数须等于 boardSize");
            }
            for (int c = 0; c < size; c++) {
                int v = board[r][c];
                if (v != Stone.EMPTY && v != Stone.BLACK && v != Stone.WHITE) {
                    throw new IllegalArgumentException("棋盘格须为 0、1、2");
                }
            }
        }
    }
}
