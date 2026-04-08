package com.gomoku.sync.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GameHistoryServiceTest {

    @Test
    void normalizeResultFilter() {
        assertNull(GameHistoryService.normalizeResultFilter(null));
        assertNull(GameHistoryService.normalizeResultFilter(""));
        assertNull(GameHistoryService.normalizeResultFilter("  "));
        assertNull(GameHistoryService.normalizeResultFilter("all"));
        assertEquals("WIN", GameHistoryService.normalizeResultFilter("win"));
        assertEquals("LOSS", GameHistoryService.normalizeResultFilter("LOSS"));
    }

    @Test
    void resolveMyResult_blackWin() {
        assertEquals("WIN", GameHistoryService.resolveMyResult(10L, 10L, 20L, "BLACK_WIN"));
        assertEquals("LOSS", GameHistoryService.resolveMyResult(20L, 10L, 20L, "BLACK_WIN"));
    }

    @Test
    void resolveMyResult_whiteWin() {
        assertEquals("LOSS", GameHistoryService.resolveMyResult(10L, 10L, 20L, "WHITE_WIN"));
        assertEquals("WIN", GameHistoryService.resolveMyResult(20L, 10L, 20L, "WHITE_WIN"));
    }

    @Test
    void resolveMyResult_draw() {
        assertEquals("DRAW", GameHistoryService.resolveMyResult(10L, 10L, 20L, "DRAW"));
    }
}
