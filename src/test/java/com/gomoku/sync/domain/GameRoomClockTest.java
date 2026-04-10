package com.gomoku.sync.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameRoomClockTest {

    @Test
    void move_timeout_wins_for_opponent() {
        GameRoom room = new GameRoom("r1", 15, "bt", 1L);
        room.setWhiteToken("wt");
        room.setWhiteUserId(2L);
        assertTrue(room.getClockMoveDeadlineWallMs() > 0);
        assertEquals(0L, room.getClockGameDeadlineWallMs());
        assertNull(room.tryMove(Stone.BLACK, 7, 7));
        assertTrue(room.getClockGameDeadlineWallMs() > 0);
        long past = System.currentTimeMillis() - GameRoom.CLOCK_MOVE_MS - 1;
        room.getLock().lock();
        try {
            java.lang.reflect.Field f = GameRoom.class.getDeclaredField("clockMoveDeadlineWallMs");
            f.setAccessible(true);
            f.setLong(room, past);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            room.getLock().unlock();
        }
        assertTrue(room.applyClockTimeoutsIfDue());
        assertTrue(room.isGameOver());
        assertEquals(Stone.BLACK, room.getWinner());
        assertEquals(GameRoom.END_REASON_MOVE_TIMEOUT, room.getGameEndReason());
    }

    @Test
    void game_time_draw_before_move_timeout() {
        GameRoom room = new GameRoom("r2", 15, "bt", 1L);
        room.setWhiteToken("wt");
        room.setWhiteUserId(2L);
        assertNull(room.tryMove(Stone.BLACK, 7, 7));
        long pastGame = System.currentTimeMillis() - GameRoom.CLOCK_GAME_MS - 1;
        room.getLock().lock();
        try {
            java.lang.reflect.Field fg = GameRoom.class.getDeclaredField("clockGameDeadlineWallMs");
            fg.setAccessible(true);
            fg.setLong(room, pastGame);
            java.lang.reflect.Field fm = GameRoom.class.getDeclaredField("clockMoveDeadlineWallMs");
            fm.setAccessible(true);
            fm.setLong(room, System.currentTimeMillis() + 30_000L);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            room.getLock().unlock();
        }
        assertTrue(room.applyClockTimeoutsIfDue());
        assertTrue(room.isGameOver());
        assertNull(room.getWinner());
        assertEquals(GameRoom.END_REASON_TIME_DRAW, room.getGameEndReason());
    }

    @Test
    void undo_request_pauses_clock_flag() {
        GameRoom room = new GameRoom("r3", 15, "bt", 1L);
        room.setWhiteToken("wt");
        room.setWhiteUserId(2L);
        assertNull(room.tryMove(Stone.BLACK, 7, 7));
        assertNull(room.tryMove(Stone.WHITE, 7, 8));
        assertNull(room.requestUndo(Stone.BLACK));
        assertTrue(room.isClockPaused());
        long past = System.currentTimeMillis() - GameRoom.CLOCK_MOVE_MS - 1;
        room.getLock().lock();
        try {
            java.lang.reflect.Field f = GameRoom.class.getDeclaredField("clockMoveDeadlineWallMs");
            f.setAccessible(true);
            f.setLong(room, past);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            room.getLock().unlock();
        }
        assertFalse(room.applyClockTimeoutsIfDue());
    }
}
