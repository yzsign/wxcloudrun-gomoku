package com.gomoku.sync.service.rating;

import com.gomoku.sync.domain.User;

/**
 * 与 rule.md §5、小程序 {@code ratingTitle.js} 中 {@code getRankAndTitleByElo} 称号分段一致；变更分数后须同步
 * {@link User#setTitleName(String)} 并随 {@code user_rating.title_name} 落库。
 */
public final class RatingTitleUtil {

    private RatingTitleUtil() {
    }

    /**
     * 根据当前天梯分得到称号（二字/三字雅称）。
     */
    public static String titleNameForElo(int elo) {
        if (elo < 1000) {
            return "木野狐";
        }
        if (elo < 1200) {
            return "石枰客";
        }
        if (elo < 1400) {
            return "玄素生";
        }
        if (elo < 1600) {
            return "落子星";
        }
        if (elo < 1800) {
            return "通幽手";
        }
        if (elo < 2000) {
            return "坐照客";
        }
        if (elo < 2200) {
            return "入神师";
        }
        if (elo < 2350) {
            return "玉楸子";
        }
        if (elo < 2500) {
            return "璇玑使";
        }
        if (elo < 2700) {
            return "天元君";
        }
        if (elo < 2900) {
            return "无极圣";
        }
        return "棋鬼王";
    }

    /**
     * 与 {@link #titleNameForElo(int)} 分段一致，返回 0～11 的段位序（与 ratingTitle.js 中称号档位一一对应），供随机匹配等逻辑按「段位差」比较。
     */
    public static int rankIndexForElo(int elo) {
        if (elo < 1000) {
            return 0;
        }
        if (elo < 1200) {
            return 1;
        }
        if (elo < 1400) {
            return 2;
        }
        if (elo < 1600) {
            return 3;
        }
        if (elo < 1800) {
            return 4;
        }
        if (elo < 2000) {
            return 5;
        }
        if (elo < 2200) {
            return 6;
        }
        if (elo < 2350) {
            return 7;
        }
        if (elo < 2500) {
            return 8;
        }
        if (elo < 2700) {
            return 9;
        }
        if (elo < 2900) {
            return 10;
        }
        return 11;
    }

    /**
     * 按 {@link User#getEloScore()} 写入内存中的称号，供 {@code updateRatingProfile} 持久化。
     */
    public static void applyTitleNameFromElo(User u) {
        if (u == null) {
            return;
        }
        u.setTitleName(titleNameForElo(u.getEloScore()));
    }
}
