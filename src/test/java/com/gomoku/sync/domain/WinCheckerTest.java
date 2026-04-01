package com.gomoku.sync.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WinCheckerTest {

    @Test
    void fiveInRowDetected() {
        int size = 15;
        int[][] b = new int[size][size];
        for (int c = 0; c < 5; c++) {
            b[7][c] = Stone.BLACK;
        }
        assertTrue(WinChecker.checkWin(b, size, 7, 4, Stone.BLACK));
    }

    @Test
    void notWinWhenBroken() {
        int size = 15;
        int[][] b = new int[size][size];
        b[0][0] = Stone.BLACK;
        b[0][1] = Stone.BLACK;
        assertFalse(WinChecker.checkWin(b, size, 0, 1, Stone.BLACK));
    }
}
