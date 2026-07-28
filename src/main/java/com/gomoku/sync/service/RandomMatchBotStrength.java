package com.gomoku.sync.service;

import com.gomoku.sync.service.rating.RatingTitleUtil;

/**
 * 随机匹配超时接入的人机：按房主<strong>段位序</strong>（0～11，与称号档位一致）定本局算棋强度；
 * 账号从同段位带（可逐步放宽）中随机选取，不再按 users.bot_search_depth_* 筛选或合并。
 */
public final class RandomMatchBotStrength {

    private static final int ABS_MAX = 12;
    private static final int ABS_MIN = 5;

    private RandomMatchBotStrength() {}

    /** 房主天梯分 → 段位序 */
    public static int hostRankIndex(int hostElo) {
        return RatingTitleUtil.rankIndexForElo(hostElo);
    }

    /**
     * 本局人机 minimax 深度 [0]=min [1]=max（含），仅由段位序决定，与 bot 账号 DB 深度字段无关。
     */
    public static int[] depthRangeForHostRank(int rankIndex) {
        int rank = rankIndex;
        if (rank < 0) {
            rank = 0;
        }
        if (rank > 11) {
            rank = 11;
        }
        int dmin;
        int dmax;
        switch (rank) {
            case 0:
                dmin = 5;
                dmax = 7;
                break;
            case 1:
                dmin = 5;
                dmax = 8;
                break;
            case 2:
                dmin = 6;
                dmax = 8;
                break;
            case 3:
                dmin = 7;
                dmax = 9;
                break;
            case 4:
                dmin = 7;
                dmax = 10;
                break;
            case 5:
                dmin = 8;
                dmax = 10;
                break;
            case 6:
                dmin = 8;
                dmax = 11;
                break;
            case 7:
                dmin = 9;
                dmax = 11;
                break;
            case 8:
                dmin = 9;
                dmax = 12;
                break;
            case 9:
            case 10:
                dmin = 10;
                dmax = 12;
                break;
            default:
                dmin = 11;
                dmax = 12;
                break;
        }
        dmin = clampDepth(dmin);
        dmax = clampDepth(Math.max(dmax, dmin));
        return new int[] {dmin, dmax};
    }

    /** 段位带 [minRank, maxRank]（含）对应的天梯分闭开区间 [minElo, maxElo)。 */
    public static int[] eloBandForRankSpan(int minRank, int maxRank) {
        int lo = Math.max(0, Math.min(minRank, maxRank));
        int hi = Math.min(11, Math.max(minRank, maxRank));
        return new int[] {
            RatingTitleUtil.minEloInclusiveForRankIndex(lo),
            RatingTitleUtil.maxEloExclusiveForRankIndex(hi)
        };
    }

    private static int clampDepth(int d) {
        if (d < ABS_MIN) {
            return ABS_MIN;
        }
        return Math.min(d, ABS_MAX);
    }
}
