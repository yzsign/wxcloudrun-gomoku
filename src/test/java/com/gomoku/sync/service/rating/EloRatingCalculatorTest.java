package com.gomoku.sync.service.rating;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EloRatingCalculatorTest {

    @Test
    void upsetBonusLowWinBigGap() {
        assertEquals(20, EloRatingCalculator.upsetBonus(1300, 1900));
        assertEquals(0, EloRatingCalculator.upsetBonus(1500, 1700));
    }

    @Test
    void mValueBySteps() {
        assertEquals(1.0, EloRatingCalculator.mValue(30), 0.001);
        assertEquals(0.8, EloRatingCalculator.mValue(20), 0.001);
        assertEquals(0.5, EloRatingCalculator.mValue(10), 0.001);
    }

    @Test
    void kValueBands() {
        assertEquals(40, EloRatingCalculator.kValue(1399, false));
        assertEquals(30, EloRatingCalculator.kValue(1800, false));
        assertEquals(20, EloRatingCalculator.kValue(2200, false));
        assertEquals(15, EloRatingCalculator.kValue(2500, false));
        assertEquals(15, EloRatingCalculator.kValue(1800, true));
    }

    @Test
    void exampleRoughlyLikeRuleDoc1300vs1900Win() {
        int d = EloRatingCalculator.delta(1300, 1900, 1.0, 40, 0, 0, false, false);
        d += EloRatingCalculator.upsetBonus(1300, 1900);
        assertTrue(d >= 55 && d <= 65, "expect ~39+20, got " + d);
    }

    @Test
    void drawChangesEloWhenExpectedNotHalf() {
        int hi = EloRatingCalculator.delta(1700, 1300, 0.5, 30, 0, 0, false, false, 1.0);
        int lo = EloRatingCalculator.delta(1300, 1700, 0.5, 30, 0, 0, false, false, 1.0);
        assertTrue(hi < 0, "高分方和棋应略低于预期胜率，扣分: " + hi);
        assertTrue(lo > 0, "低分方和棋应略高于预期胜率，加分: " + lo);
    }

    @Test
    void friendKScaleReducesCoreDelta() {
        int full = EloRatingCalculator.delta(1500, 1500, 1.0, 40, 0, 0, false, false, 1.0);
        int half = EloRatingCalculator.delta(1500, 1500, 1.0, 40, 0, 0, false, false, 0.5);
        assertTrue(half < full && full > 0, "0.5 倍 K 时变动应小于全量 K");
        assertTrue(Math.abs(half * 2.0 / full - 1.0) < 0.2, "约一半，因四舍五入可略有偏差: full=" + full + " half=" + half);
    }
}
