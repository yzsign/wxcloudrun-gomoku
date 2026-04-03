package com.gomoku.sync.service.rating;

/**
 * rule.md §3：K/M/C、预期胜率 E、越级奖励 §6.2。
 */
public final class EloRatingCalculator {

    private EloRatingCalculator() {
    }

    public static int kValue(int elo, boolean lowTrust) {
        int k;
        if (elo < 1400) {
            k = 40;
        } else if (elo < 2000) {
            k = 30;
        } else if (elo < 2400) {
            k = 20;
        } else {
            k = 15;
        }
        if (lowTrust) {
            k = k / 2;
        }
        return k;
    }

    public static double mValue(int totalSteps) {
        if (totalSteps >= 30) {
            return 1.0;
        }
        if (totalSteps >= 15) {
            return 0.8;
        }
        return 0.5;
    }

    /**
     * @param consecutiveWins  本局开始前连胜场数（不含本局）
     * @param consecutiveLosses 本局开始前连败场数（不含本局）
     * @param result            本局结果：1 胜 0 负 0.5 平
     */
    public static double cValue(int consecutiveWins, int consecutiveLosses, double result) {
        if (result == 0.5) {
            return 1.0;
        }
        if (result > 0.5) {
            if (consecutiveWins >= 3) {
                double c = 1.1 + 0.02 * (consecutiveWins - 3);
                return Math.min(1.3, c);
            }
        } else {
            if (consecutiveLosses >= 3) {
                double c = 1.1 + 0.02 * (consecutiveLosses - 3);
                return Math.min(1.3, c);
            }
        }
        return 1.0;
    }

    public static double expectedScore(int selfElo, int opponentElo) {
        double p = (opponentElo - selfElo) / 400.0;
        return 1.0 / (1.0 + Math.pow(10, p));
    }

    public static int roundDelta(double raw) {
        return (int) Math.round(raw);
    }

    public static int delta(
            int selfElo,
            int opponentElo,
            double result,
            int totalSteps,
            int consecutiveWins,
            int consecutiveLosses,
            boolean lowTrust,
            boolean forceM1ForThisSide) {
        double e = expectedScore(selfElo, opponentElo);
        int k = kValue(selfElo, lowTrust);
        double m = forceM1ForThisSide && result >= 0.5 ? 1.0 : mValue(totalSteps);
        double c = cValue(consecutiveWins, consecutiveLosses, result);
        return roundDelta(k * (result - e) * m * c);
    }

    /** §6.2 低分方胜且分差 ≥ 400 */
    public static int upsetBonus(int winnerElo, int loserElo) {
        if (winnerElo >= loserElo) {
            return 0;
        }
        int diff = loserElo - winnerElo;
        if (diff < 400) {
            return 0;
        }
        return Math.min(20, diff / 20);
    }
}
