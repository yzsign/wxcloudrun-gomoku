package com.gomoku.sync.service.rating;

import com.gomoku.sync.domain.User;

import java.time.LocalDate;

/**
 * rule.md §7.1：单日净变动 ±150，溢出进 elo_carry_over，次日 rollover 并入 elo_score。
 */
public final class DailyEloCap {

    public static final int DAILY_NET_CAP = 150;

    private DailyEloCap() {
    }

    /** 新自然日：释放 carry，重置当日统计 */
    public static void rolloverIfNeeded(User u, LocalDate today) {
        LocalDate last = u.getLastRatingResetDate();
        if (last == null || !last.equals(today)) {
            int elo = u.getEloScore() + u.getEloCarryOver();
            u.setEloScore(Math.max(100, elo));
            u.setEloCarryOver(0);
            u.setTodayNetChange(0);
            u.setLastRatingResetDate(today);
        }
    }

    /**
     * 将「理论积分变化」应用到用户：受单日净变动上限约束，并更新 elo_score。
     *
     * @return 实际加到 elo_score 上的量（不含 rollover 中的 carry）
     */
    public static int applyNetChange(User u, int rawDelta) {
        int net = u.getTodayNetChange();
        int newNet = net + rawDelta;
        int appliedToElo;
        if (newNet > DAILY_NET_CAP) {
            int overflow = newNet - DAILY_NET_CAP;
            appliedToElo = rawDelta - overflow;
            u.setEloCarryOver(u.getEloCarryOver() + overflow);
            u.setTodayNetChange(DAILY_NET_CAP);
        } else if (newNet < -DAILY_NET_CAP) {
            int overflow = newNet - (-DAILY_NET_CAP);
            appliedToElo = rawDelta - overflow;
            u.setEloCarryOver(u.getEloCarryOver() + overflow);
            u.setTodayNetChange(-DAILY_NET_CAP);
        } else {
            appliedToElo = rawDelta;
            u.setTodayNetChange(newNet);
        }
        u.setEloScore(Math.max(100, u.getEloScore() + appliedToElo));
        return appliedToElo;
    }
}
