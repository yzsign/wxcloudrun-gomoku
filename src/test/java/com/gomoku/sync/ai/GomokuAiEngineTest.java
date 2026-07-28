package com.gomoku.sync.ai;

import com.gomoku.sync.domain.Stone;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GomokuAiEngineTest {

    private static int[][] emptyBoard(int size) {
        return new int[size][size];
    }

    @Test
    void emptyBoard_blackFirstMoveVariesAmongStarPoints() {
        int size = 15;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 80; i++) {
            int[] mv =
                    GomokuAiEngine.chooseMove(
                            emptyBoard(size), size, Stone.BLACK, 10, BotAiStyle.BALANCED);
            seen.add(mv[0] + "," + mv[1]);
            assertTrue(mv[0] >= 5 && mv[0] <= 9 && mv[1] >= 5 && mv[1] <= 9, "black 1 in center 5x5");
        }
        assertTrue(seen.size() >= 3, "expected varied black openings, got " + seen);
    }

    private static int[][] boardWithBlackCenter(int size) {
        int[][] board = new int[size][size];
        int c = size / 2;
        board[c][c] = Stone.BLACK;
        return board;
    }

    /**
     * 随机匹配人机搜索深度 7+ 时仍应走定式库（白 2 在天元八邻随机），不能每局固定同一应手。
     */
    @Test
    void deepSearch_stillUsesOpeningBookForWhiteSecondMove() {
        int size = 15;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 40; i++) {
            int[][] board = boardWithBlackCenter(size);
            int[] mv =
                    GomokuAiEngine.chooseMove(
                            board, size, Stone.WHITE, 10, BotAiStyle.BALANCED);
            seen.add(mv[0] + "," + mv[1]);
        }
        assertTrue(seen.size() >= 2, "expected varied white second moves, got " + seen);
    }

    /**
     * 连续冲四杀：黑 7,5–7,8 四子，白仅堵 7,9；黑 7,4 冲四后白必堵 7,10，黑再 7,11 成五（VCF 首着 7,4）。
     */
    @Test
    void vcf_picksFirstForcingRushFour() {
        int size = 15;
        int[][] board = emptyBoard(size);
        int row = 7;
        board[row][5] = Stone.BLACK;
        board[row][6] = Stone.BLACK;
        board[row][7] = Stone.BLACK;
        board[row][8] = Stone.BLACK;
        board[row][9] = Stone.WHITE;
        int[] mv =
                GomokuAiEngine.chooseMove(
                        board, size, Stone.BLACK, 8, BotAiStyle.BALANCED);
        assertTrue(mv[0] == row && mv[1] == 4, "expected VCF opener at 7,4 got " + mv[0] + "," + mv[1]);
    }
}
