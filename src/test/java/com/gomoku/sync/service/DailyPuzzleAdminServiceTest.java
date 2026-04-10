package com.gomoku.sync.service;

import com.gomoku.sync.domain.Stone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DailyPuzzleAdminServiceTest {

    @Test
    void validateBoardCells_acceptsEmpty15() {
        int[][] b = new int[15][15];
        assertDoesNotThrow(() -> DailyPuzzleAdminService.validateBoardCells(b, 15));
    }

    @Test
    void validateBoardCells_rejectsBadValue() {
        int[][] b = new int[15][15];
        b[0][0] = 3;
        assertThrows(IllegalArgumentException.class, () -> DailyPuzzleAdminService.validateBoardCells(b, 15));
    }

    @Test
    void validateBoardCells_rejectsWrongSize() {
        int[][] b = new int[15][14];
        assertThrows(IllegalArgumentException.class, () -> DailyPuzzleAdminService.validateBoardCells(b, 15));
    }

    @Test
    void validateBoardCells_acceptsStones() {
        int[][] b = new int[15][15];
        b[7][7] = Stone.BLACK;
        b[7][8] = Stone.WHITE;
        assertDoesNotThrow(() -> DailyPuzzleAdminService.validateBoardCells(b, 15));
    }
}
