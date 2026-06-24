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

    /**
     * 随机匹配：仅一方在位时应暂停读秒；否则建房时的步时会在「等对手」期间触发空盘超时终局。
     */
    @Test
    void random_match_paused_until_both_seats_no_move_timeout_on_empty_board() {
        GameRoom room = new GameRoom("rm1", 15, "bt", 1L);
        room.setRandomMatch(true);
        room.setWhiteToken("wt");
        room.setWhiteUserId(2L);
        room.setClusterBlackConnected(true);
        assertFalse(room.isClockPaused());
        room.syncFriendRoomClockPauseForLiveSeats();
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
        assertFalse(room.isGameOver());
    }

    /** 防御：盘面尚无落子时，即使步时截止已过也不判 MOVE_TIMEOUT（与 syncFriendRoomClockPause 双保险） */
    @Test
    void move_timeout_ignored_when_no_moves_yet_even_if_deadline_passed() {
        GameRoom room = new GameRoom("emptyBt", 15, "bt", 1L);
        room.setWhiteToken("wt");
        room.setWhiteUserId(2L);
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
        assertFalse(room.isGameOver());
        assertNull(room.getGameEndReason());
    }
}
