package com.gomoku.sync.domain;

import com.gomoku.sync.api.dto.GameMoveDto;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DailyPuzzleReplayTest {

    @Test
    void winInOne_blackConnectFive() {
        int[][] b = new int[15][15];
        int r = 7;
        for (int c = 4; c <= 7; c++) {
            b[r][c] = Stone.BLACK;
        }
        List<GameMoveDto> moves = Collections.singletonList(new GameMoveDto(7, 8, Stone.BLACK));
        assertEquals(
                DailyPuzzleReplay.Result.SOLVED,
                DailyPuzzleReplay.evaluate(b, 15, Stone.BLACK, DailyPuzzle.GOAL_WIN, null, moves));
    }

    @Test
    void wrongColor_invalid() {
        int[][] b = new int[15][15];
        b[0][0] = Stone.BLACK;
        List<GameMoveDto> moves = Collections.singletonList(new GameMoveDto(0, 1, Stone.BLACK));
        assertEquals(
                DailyPuzzleReplay.Result.INVALID,
                DailyPuzzleReplay.evaluate(b, 15, Stone.WHITE, DailyPuzzle.GOAL_WIN, null, moves));
    }
}
