package com.gomoku.sync.domain;

/**
 * 落子后是否五连（与前端 gomoku.js checkWin 一致）
 */
public final class WinChecker {

    private static final int[][] DIRS = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};

    private WinChecker() {}

    public static boolean checkWin(int[][] board, int size, int r, int c, int color) {
        for (int[] dir : DIRS) {
            int dr = dir[0];
            int dc = dir[1];
            int count = 1;
            for (int step = 1; step < 5; step++) {
                int nr = r + dr * step;
                int nc = c + dc * step;
                if (nr >= 0 && nr < size && nc >= 0 && nc < size && board[nr][nc] == color) {
                    count++;
                } else {
                    break;
                }
            }
            for (int s = 1; s < 5; s++) {
                int nr2 = r - dr * s;
                int nc2 = c - dc * s;
                if (nr2 >= 0 && nr2 < size && nc2 >= 0 && nc2 < size && board[nr2][nc2] == color) {
                    count++;
                } else {
                    break;
                }
            }
            if (count >= 5) {
                return true;
            }
        }
        return false;
    }

    public static boolean boardFull(int[][] board, int size) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == Stone.EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }
}
