package com.gomoku.sync.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PveGameServiceTest {

    @Test
    void resolveOutcome_blackPerspective() {
        assertEquals(
                GameHistoryService.OUTCOME_BLACK_WIN,
                PveGameService.resolveOutcome(true, "WIN"));
        assertEquals(
                GameHistoryService.OUTCOME_WHITE_WIN,
                PveGameService.resolveOutcome(true, "LOSS"));
        assertEquals(
                GameHistoryService.OUTCOME_DRAW,
                PveGameService.resolveOutcome(true, "DRAW"));
    }

    @Test
    void resolveOutcome_whitePerspective() {
        assertEquals(
                GameHistoryService.OUTCOME_WHITE_WIN,
                PveGameService.resolveOutcome(false, "WIN"));
        assertEquals(
                GameHistoryService.OUTCOME_BLACK_WIN,
                PveGameService.resolveOutcome(false, "LOSS"));
    }
}
