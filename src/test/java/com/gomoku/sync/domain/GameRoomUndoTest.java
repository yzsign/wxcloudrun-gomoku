package com.gomoku.sync.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 悔棋：一手只撤申请方上一子；对方已应手时可申请两手，同意则连撤两手。 */
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

    @Test
    void acceptTwoPlyUndoAfterOpponentRepliedRemovesBothStones() {
        GameRoom room = new GameRoom("r3", 15, "bt", 1L);
        room.setWhiteToken("wt");
        room.setWhiteUserId(2L);

        assertNull(room.tryMove(Stone.BLACK, 7, 7));
        assertNull(room.tryMove(Stone.WHITE, 7, 8));

        assertNull(room.requestUndo(Stone.BLACK));
        assertNull(room.acceptUndo(Stone.WHITE));

        int[][] b = room.getBoardCopy();
        assertEquals(Stone.EMPTY, b[7][7]);
        assertEquals(Stone.EMPTY, b[7][8]);
    }

    @Test
    void secondUndoRequestWithin10SecondsAfterRejectIsBlocked() {
        GameRoom room = new GameRoom("r4", 15, "bt", 1L);
        room.setWhiteToken("wt");
        room.setWhiteUserId(2L);

        assertNull(room.tryMove(Stone.BLACK, 7, 7));
        assertNull(room.tryMove(Stone.WHITE, 7, 8));
        assertNull(room.requestUndo(Stone.WHITE));
        assertNull(room.rejectUndo(Stone.BLACK));
        assertEquals("10秒内不可再次发起悔棋", room.requestUndo(Stone.WHITE));
    }
}
