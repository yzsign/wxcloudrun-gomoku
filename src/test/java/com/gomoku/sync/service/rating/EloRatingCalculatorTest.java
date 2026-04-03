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
}
