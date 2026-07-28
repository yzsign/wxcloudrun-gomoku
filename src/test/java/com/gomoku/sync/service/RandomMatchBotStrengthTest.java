package com.gomoku.sync.service;

import com.gomoku.sync.service.rating.RatingTitleUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomMatchBotStrengthTest {

    @Test
    void higherRankGetsDeeperSearch() {
        int[] low = RandomMatchBotStrength.depthRangeForHostRank(1);
        int[] high = RandomMatchBotStrength.depthRangeForHostRank(8);
        assertTrue(high[0] >= low[0]);
        assertTrue(high[1] >= low[1]);
    }

    @Test
    void topRankNearEngineCap() {
        assertArrayEquals(new int[] {11, 12}, RandomMatchBotStrength.depthRangeForHostRank(11));
    }

    @Test
    void hostRankFromEloAlignsWithTitleUtil() {
        int elo = 2400;
        assertEquals(RatingTitleUtil.rankIndexForElo(elo), RandomMatchBotStrength.hostRankIndex(elo));
    }

    @Test
    void rankSpanBandCoversSingleTier() {
        int[] band = RandomMatchBotStrength.eloBandForRankSpan(2, 2);
        assertEquals(1200, band[0]);
        assertEquals(1400, band[1]);
    }
}
