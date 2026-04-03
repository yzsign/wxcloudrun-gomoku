package com.gomoku.sync.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 悔棋：同意时只撤回申请方上一手，不得连撤两手。
 */
class GameRoomUndoTest {

    @Test
    void acceptUndoRemovesOnlyRequesterLastStone() {
        GameRoom room = new GameRoom("r1", 15, "bt", 1L);
        room.setWhiteToken("wt");
        room.setWhiteUserId(2L);

        assertNull(room.tryMove(Stone.BLACK, 7, 7));
        assertNull(room.tryMove(Stone.WHITE, 7, 8));

        assertNull(room.requestUndo(Stone.WHITE));
        assertNull(room.acceptUndo(Stone.BLACK));

        int[][] b = room.getBoardCopy();
        assertEquals(Stone.BLACK, b[7][7]);
        assertEquals(Stone.EMPTY, b[7][8]);
    }

    @Test
    void acceptUndoAfterSeveralMovesRemovesOnlyOneStone() {
        GameRoom room = new GameRoom("r2", 15, "bt", 1L);
        room.setWhiteToken("wt");
        room.setWhiteUserId(2L);

        assertNull(room.tryMove(Stone.BLACK, 7, 7));
        assertNull(room.tryMove(Stone.WHITE, 7, 8));
        assertNull(room.tryMove(Stone.BLACK, 7, 9));
        assertNull(room.tryMove(Stone.WHITE, 6, 8));

        assertNull(room.requestUndo(Stone.WHITE));
        assertNull(room.acceptUndo(Stone.BLACK));

        int[][] b = room.getBoardCopy();
        assertEquals(Stone.BLACK, b[7][7]);
        assertEquals(Stone.WHITE, b[7][8]);
        assertEquals(Stone.BLACK, b[7][9]);
        assertEquals(Stone.EMPTY, b[6][8]);
    }
}
