package com.gomoku.sync.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameRoomResignTest {

    @Test
    void resignBlackYieldsWhiteWinner() {
        GameRoom room = new GameRoom("r1", 15, "bt", 1L);
        room.setWhiteToken("wt");
        room.setWhiteUserId(2L);

        assertNull(room.tryMove(Stone.BLACK, 7, 7));
        assertNull(room.tryResign(Stone.BLACK));
        assertTrue(room.isGameOver());
        assertEquals(Stone.WHITE, room.getWinner());
        assertEquals(GameRoom.END_REASON_RESIGN, room.getGameEndReason());
    }

    @Test
    void resignAfterGameOverFails() {
        GameRoom room = new GameRoom("r2", 15, "bt", 1L);
        room.setWhiteToken("wt");
        room.setWhiteUserId(2L);

        assertNull(room.tryResign(Stone.BLACK));
        assertEquals("对局已结束", room.tryResign(Stone.WHITE));
    }
}
