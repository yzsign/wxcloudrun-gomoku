package com.gomoku.sync.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameRoomDrawTest {

    @Test
    void draw_accept_ends_in_draw() {
        GameRoom room = new GameRoom("r1", 15, "bt", 1L);
        room.setWhiteToken("wt");
        room.setWhiteUserId(2L);
        assertNull(room.requestDraw(Stone.BLACK));
        assertTrue(room.isDrawPending());
        assertNull(room.acceptDraw(Stone.WHITE));
        assertTrue(room.isGameOver());
        assertNull(room.getWinner());
        assertFalse(room.isDrawPending());
    }

    @Test
    void draw_reject_clears_pending() {
        GameRoom room = new GameRoom("r2", 15, "bt", 1L);
        room.setWhiteToken("wt");
        room.setWhiteUserId(2L);
        assertNull(room.requestDraw(Stone.WHITE));
        assertNull(room.rejectDraw(Stone.BLACK));
        assertFalse(room.isDrawPending());
        assertFalse(room.isGameOver());
    }

    @Test
    void undo_blocked_while_draw_pending() {
        GameRoom room = new GameRoom("r3", 15, "bt", 1L);
        room.setWhiteToken("wt");
        room.setWhiteUserId(2L);
        assertNull(room.tryMove(Stone.BLACK, 7, 7));
        assertNull(room.requestDraw(Stone.WHITE));
        assertEquals("请先处理和棋申请", room.requestUndo(Stone.BLACK));
    }
}
