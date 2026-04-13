package com.gomoku.sync.ai;

import com.gomoku.sync.domain.Stone;
import com.gomoku.sync.domain.WinChecker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 五子棋人机落子（必胜/必堵 + 候选裁剪 + minimax）。
 * 支持 {@link BotAiStyle}：不同评估与排序权重，以及「多变」风格在相近最优解中随机。
 */
public final class GomokuAiEngine {

    private static final int[][] DIRS = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
    /** 未指定 DB 深度时的默认 minimax 层数（叶为 0） */
    private static final int DEFAULT_SEARCH_DEPTH = 4;
    /** 人机深度全局上限，防止 DB 误配极大值 */
    private static final int ABS_MAX_BOT_SEARCH_DEPTH = 8;
    /** 根节点与内层候选上限（内层另见 {@link #maxCandForDepth}） */
    private static final int MAX_CANDIDATES_ROOT = 18;
    /** 单步思考时间上限（纳秒），超时则叶节点提前返回局面分 */
    private static final long MOVE_TIME_BUDGET_NANOS = 70_000_000L;
    /** 实际搜索深度上限（与 DB 取 min） */
    private static final int EFFECTIVE_DEPTH_CAP = 6;
    /** 对手威胁权重（文档规则3），与 gomoku.js OPP_LINE_PATTERN_MULT 一致 */
    private static final double OPP_LINE_PATTERN_MULT = 1.3;
    /**
     * 候选排序：对方在该点若落子的棋型威胁再乘此系数（与前端 moveOrderingScore 中防守侧一致）
     */
    private static final double ORDER_OPP_BLOCK_WEIGHT = 1.3;
    /** 双威胁对进攻分的倍数（文档规则2） */
    private static final int DOUBLE_THREAT_ATTACK_MULT = 4;
    /** 强制优先级最高执行到第几层（3–7，7 为防对方双活二），与前端 gomoku.js 一致 */
    private static final int FORCED_TIERS_MAX = 7;
    /** 双活二棋型威胁下限（与 gomoku.js shapeThreatScore 一致） */
    private static final double DOUBLE_LIVE_TWO_SHAPE_BUMP = 220.0;

    private static final ThreadLocal<Long> DEADLINE_NANOS = new ThreadLocal<>();
    private static final ThreadLocal<BotAiStyle> AI_STYLE = new ThreadLocal<>();

    private GomokuAiEngine() {}

    /**
     * 人机每步在 [min, max] 间随机 minimax 搜索深度（与 users.bot_search_depth_* 对应；必胜/必堵仍优先）。
     */
    public static int nextBotSearchDepthInRange(int min, int max) {
        if (min > max) {
            int t = min;
            min = max;
            max = t;
        }
        min = Math.min(Math.max(1, min), ABS_MAX_BOT_SEARCH_DEPTH);
        max = Math.min(Math.max(1, max), ABS_MAX_BOT_SEARCH_DEPTH);
        if (min > max) {
            int t = min;
            min = max;
            max = t;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static int[] chooseMove(int[][] board, int size, int aiColor) {
        return chooseMove(board, size, aiColor, DEFAULT_SEARCH_DEPTH, BotAiStyle.BALANCED);
    }

    public static int[] chooseMove(int[][] board, int size, int aiColor, int searchDepth) {
        return chooseMove(board, size, aiColor, searchDepth, BotAiStyle.BALANCED);
    }

    public static int[] chooseMove(
            int[][] board, int size, int aiColor, int searchDepth, BotAiStyle style) {
        if (style == null) {
            style = BotAiStyle.BALANCED;
        }
        if (searchDepth < 1) {
            searchDepth = 1;
        }
        searchDepth = Math.min(searchDepth, EFFECTIVE_DEPTH_CAP);
        long deadline = System.nanoTime() + MOVE_TIME_BUDGET_NANOS;
        DEADLINE_NANOS.set(deadline);
        AI_STYLE.set(style);
        try {
            return chooseMoveInner(board, size, aiColor, searchDepth);
        } finally {
            DEADLINE_NANOS.remove();
            AI_STYLE.remove();
        }
    }

    private static BotAiStyle style() {
        BotAiStyle s = AI_STYLE.get();
        return s != null ? s : BotAiStyle.BALANCED;
    }

    private static boolean timeUp() {
        Long d = DEADLINE_NANOS.get();
        return d != null && System.nanoTime() > d;
    }

    private static int[] chooseMoveInner(int[][] board, int size, int aiColor, int searchDepth) {
        BotAiStyle st = style();
        int opp = opposite(aiColor);
        int stones = countStones(board, size);
        if (stones == 0) {
            return new int[]{size / 2, size / 2};
        }

        int[] win = findWinningMove(board, size, aiColor);
        if (win != null) {
            return win;
        }
        int[] block = findWinningMove(board, size, opp);
        if (block != null) {
            return block;
        }

        int[] forced = findForcedPriorityMove(board, size, aiColor);
        if (forced != null) {
            return forced;
        }

        List<int[]> cands = getCandidates(board, size);
        if (cands.isEmpty()) {
            return new int[]{size / 2, size / 2};
        }

        sortCandidates(board, size, cands, aiColor, st);

        int cap = Math.min(MAX_CANDIDATES_ROOT, cands.size());
        cands = cands.subList(0, cap);

        List<int[]> rootMoves = new ArrayList<>(cap);
        List<Integer> rootScores = new ArrayList<>(cap);
        int bestR = cands.get(0)[0];
        int bestC = cands.get(0)[1];
        int bestScore = Integer.MIN_VALUE;
        int plyDepth = searchDepth - 1;
        for (int[] m : cands) {
            if (timeUp()) {
                break;
            }
            int r = m[0];
            int c = m[1];
            if (board[r][c] != Stone.EMPTY) {
                continue;
            }
            board[r][c] = aiColor;
            if (WinChecker.checkWin(board, size, r, c, aiColor)) {
                board[r][c] = Stone.EMPTY;
                return new int[]{r, c};
            }
            int sc =
                    minimax(
                            board,
                            size,
                            plyDepth,
                            false,
                            aiColor,
                            opp,
                            Integer.MIN_VALUE,
                            Integer.MAX_VALUE,
                            searchDepth,
                            1);
            board[r][c] = Stone.EMPTY;
            rootMoves.add(m);
            rootScores.add(sc);
            if (sc > bestScore) {
                bestScore = sc;
                bestR = r;
                bestC = c;
            }
        }
        if (rootMoves.isEmpty()) {
            return new int[]{bestR, bestC};
        }
        if (st.isRootRandomAmongNearBest() && rootScores.size() > 1) {
            int margin = st.getNearBestScoreMargin();
            List<int[]> pool = new ArrayList<>();
            for (int i = 0; i < rootScores.size(); i++) {
                if (rootScores.get(i) >= bestScore - margin) {
                    pool.add(rootMoves.get(i));
                }
            }
            if (!pool.isEmpty()) {
                int[] pick = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
                return new int[]{pick[0], pick[1]};
            }
        }
        return new int[]{bestR, bestC};
    }

    private static void sortCandidates(
            int[][] board,
            int size,
            List<int[]> cands,
            int moveForColor,
            BotAiStyle st) {
        double wm = st.getOrderMyW();
        double wo = st.getOrderOppW();
        int opp = opposite(moveForColor);
        int stoneCount = countStones(board, size);
        cands.sort(
                Comparator.comparingDouble(
                                (int[] m) -> {
                                    int r = m[0];
                                    int c = m[1];
                                    MoveAnalysis my =
                                            analyzeMovePattern(board, size, r, c, moveForColor);
                                    MoveAnalysis op = analyzeMovePattern(board, size, r, c, opp);
                                    double h = my != null ? shapeThreatScore(my) : 0;
                                    double h2 = op != null ? shapeThreatScore(op) : 0;
                                    double core = wm * h + wo * (h2 * ORDER_OPP_BLOCK_WEIGHT);
                                    core += openingOrderingBonus(board, size, r, c, stoneCount);
                                    return -core;
                                })
                        .thenComparingDouble(
                                m -> -centerTieBreakScore(m[0], m[1], size)));
    }

    /** 与 gomoku.js sortMovesByHeuristic：局面前三手优先贴近已有棋子（切比雪夫距） */
    private static double openingOrderingBonus(
            int[][] board, int size, int r, int c, int stoneCount) {
        if (stoneCount > 2) {
            return 0;
        }
        int d0 = minChebyshevDistToNearestStone(board, size, r, c);
        if (d0 >= 99) {
            return 0;
        }
        return (6 - d0) * 2.0;
    }

    private static int minChebyshevDistToNearestStone(int[][] board, int size, int r, int c) {
        int best = 99;
        for (int rr = 0; rr < size; rr++) {
            for (int cc = 0; cc < size; cc++) {
                if (board[rr][cc] == Stone.EMPTY) {
                    continue;
                }
                int d = Math.max(Math.abs(rr - r), Math.abs(cc - c));
                if (d < best) {
                    best = d;
                }
            }
        }
        return best;
    }

    /**
     * 与 gomoku.js minimax 一致：{@code max(12, min(根候选上限, 6 + depth*6))}，避免浅层分支过少漏算。
     */
    private static int maxCandForDepth(int depthRemaining) {
        int cap = Math.min(MAX_CANDIDATES_ROOT, 6 + depthRemaining * 6);
        return Math.max(12, cap);
    }

    private static int minimax(
            int[][] board,
            int size,
            int depth,
            boolean maximizing,
            int aiColor,
            int opp,
            int alpha,
            int beta,
            int rootSearchDepth,
            int depthFromRoot) {
        BotAiStyle st = style();
        if (timeUp()) {
            return evaluateBoard(board, size, aiColor, st);
        }
        int turn = maximizing ? aiColor : opp;
        int[] instantWin = findWinningMove(board, size, turn);
        if (instantWin != null) {
            if (turn == aiColor) {
                return 2_000_000 - (rootSearchDepth - depth);
            }
            return -2_000_000 + (rootSearchDepth - depth);
        }
        if (depth == 0) {
            return evaluateBoard(board, size, aiColor, st);
        }
        List<int[]> cands = getCandidates(board, size);
        if (cands.isEmpty()) {
            return evaluateBoard(board, size, aiColor, st);
        }
        sortCandidates(board, size, cands, turn, st);
        int cap = Math.min(maxCandForDepth(depth), cands.size());
        cands = cands.subList(0, cap);

        if (maximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int[] m : cands) {
                if (timeUp()) {
                    return evaluateBoard(board, size, aiColor, st);
                }
                int r = m[0];
                int c = m[1];
                if (board[r][c] != Stone.EMPTY) {
                    continue;
                }
                board[r][c] = turn;
                int ev;
                if (WinChecker.checkWin(board, size, r, c, turn)) {
                    ev = 2_000_000 - (rootSearchDepth - depth);
                } else if (WinChecker.boardFull(board, size)) {
                    ev = 0;
                } else {
                    ev =
                            minimax(
                                    board,
                                    size,
                                    depth - 1,
                                    false,
                                    aiColor,
                                    opp,
                                    alpha,
                                    beta,
                                    rootSearchDepth,
                                    depthFromRoot + 1);
                }
                board[r][c] = Stone.EMPTY;
                maxEval = Math.max(maxEval, ev);
                alpha = Math.max(alpha, ev);
                if (beta <= alpha) {
                    break;
                }
            }
            return maxEval;
        }
        int minEval = Integer.MAX_VALUE;
        for (int[] m : cands) {
            if (timeUp()) {
                return evaluateBoard(board, size, aiColor, st);
            }
            int r = m[0];
            int c = m[1];
            if (board[r][c] != Stone.EMPTY) {
                continue;
            }
            board[r][c] = turn;
            int ev;
            if (WinChecker.checkWin(board, size, r, c, turn)) {
                ev = -2_000_000 + (rootSearchDepth - depth);
            } else if (WinChecker.boardFull(board, size)) {
                ev = 0;
            } else {
                ev =
                        minimax(
                                board,
                                size,
                                depth - 1,
                                true,
                                aiColor,
                                opp,
                                alpha,
                                beta,
                                rootSearchDepth,
                                depthFromRoot + 1);
            }
            board[r][c] = Stone.EMPTY;
            minEval = Math.min(minEval, ev);
            beta = Math.min(beta, ev);
            if (beta <= alpha) {
                break;
            }
        }
        return minEval;
    }

    /**
     * 沿横/竖/斜每条线扫描连续棋块，区分两端是否为空（活二/活三/活四等），再累计形势分；
     * 比原「五格窗口」更能反映防守压力。
     */
    private static int evaluateBoard(int[][] board, int size, int aiColor, BotAiStyle st) {
        double em = st.getEvalMyW();
        double eo = st.getEvalOppW();
        double s = 0;
        int[] line = new int[size];
        int r;
        int c;
        for (r = 0; r < size; r++) {
            for (c = 0; c < size; c++) {
                line[c] = board[r][c];
            }
            s += evaluateLineArray(line, size, aiColor, em, eo);
        }
        for (c = 0; c < size; c++) {
            for (r = 0; r < size; r++) {
                line[r] = board[r][c];
            }
            s += evaluateLineArray(line, size, aiColor, em, eo);
        }
        for (int d = -(size - 1); d <= size - 1; d++) {
            int count = 0;
            if (d >= 0) {
                r = d;
                c = 0;
            } else {
                r = 0;
                c = -d;
            }
            while (r < size && c < size) {
                line[count++] = board[r][c];
                r++;
                c++;
            }
            if (count > 0) {
                s += evaluateLineArray(line, count, aiColor, em, eo);
            }
        }
        for (int sumIdx = 0; sumIdx <= 2 * (size - 1); sumIdx++) {
            r = Math.max(0, sumIdx - (size - 1));
            c = sumIdx - r;
            int count = 0;
            while (r < size && c >= 0) {
                line[count++] = board[r][c];
                r++;
                c--;
            }
            if (count > 0) {
                s += evaluateLineArray(line, count, aiColor, em, eo);
            }
        }
        if (s > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (s < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) Math.round(s);
    }

    private static double evaluateLineArray(
            int[] line, int len, int aiColor, double em, double eo) {
        int opp = opposite(aiColor);
        double sum = 0;
        int i = 0;
        while (i < len) {
            if (line[i] == Stone.EMPTY) {
                i++;
                continue;
            }
            int stone = line[i];
            int j = i;
            while (j < len && line[j] == stone) {
                j++;
            }
            int runLen = j - i;
            boolean leftOpen = (i > 0 && line[i - 1] == Stone.EMPTY);
            boolean rightOpen = (j < len && line[j] == Stone.EMPTY);
            int mag = patternMagnitude(runLen, leftOpen, rightOpen);
            if (stone == aiColor) {
                sum += em * mag;
            } else if (stone == opp) {
                sum -= eo * OPP_LINE_PATTERN_MULT * mag;
            }
            i = j;
        }
        return sum;
    }

    /**
     * 与 gomoku.js patternMagnitude 一致：文档第四节量级（五连、活四、冲四、活三、活二）。
     */
    private static int patternMagnitude(int len, boolean leftOpen, boolean rightOpen) {
        int openEnds = (leftOpen ? 1 : 0) + (rightOpen ? 1 : 0);
        if (len >= 5) {
            return 100_000;
        }
        if (len == 4) {
            if (openEnds == 2) {
                return 10_000;
            }
            if (openEnds == 1) {
                return 1_000;
            }
            return 50;
        }
        if (len == 3) {
            if (openEnds == 2) {
                return 100;
            }
            if (openEnds == 1) {
                return 25;
            }
            return 2;
        }
        if (len == 2) {
            if (openEnds == 2) {
                return 5;
            }
            if (openEnds == 1) {
                return 1;
            }
            return 0;
        }
        if (len == 1) {
            if (openEnds == 2) {
                return 1;
            }
            return 0;
        }
        return 0;
    }

    private static final class LineRunInfo {
        final int len;
        final boolean leftOpen;
        final boolean rightOpen;

        LineRunInfo(int len, boolean leftOpen, boolean rightOpen) {
            this.len = len;
            this.leftOpen = leftOpen;
            this.rightOpen = rightOpen;
        }
    }

    private static final class MoveAnalysis {
        boolean hasWin;
        int nL4;
        int nR4;
        int nL3;
        int nS3;
        int nL2;
        boolean doubleRushFour;
        /** 文档规则2：两路冲四且制胜空位不同 */
        boolean independentDoubleRushFour;
        boolean doubleLiveThree;
        boolean liveThreeAndRushFour;
        /** 落子后至少两个方向为活二（双活二） */
        boolean doubleLiveTwo;
    }

    private static LineRunInfo getLineRunInfo(
            int[][] board, int size, int r, int c, int dr, int dc, int color) {
        int len = 1;
        int nr = r - dr;
        int nc = c - dc;
        while (nr >= 0 && nr < size && nc >= 0 && nc < size && board[nr][nc] == color) {
            len++;
            nr -= dr;
            nc -= dc;
        }
        int leftEnd =
                (nr >= 0 && nr < size && nc >= 0 && nc < size) ? board[nr][nc] : -1;
        nr = r + dr;
        nc = c + dc;
        while (nr >= 0 && nr < size && nc >= 0 && nc < size && board[nr][nc] == color) {
            len++;
            nr += dr;
            nc += dc;
        }
        int rightEnd =
                (nr >= 0 && nr < size && nc >= 0 && nc < size) ? board[nr][nc] : -1;
        boolean leftOpen = leftEnd == Stone.EMPTY;
        boolean rightOpen = rightEnd == Stone.EMPTY;
        return new LineRunInfo(len, leftOpen, rightOpen);
    }

    /** @return L4 / R4 / L3 / S3 / L2 / X */
    private static char classifySegment(int len, boolean leftOpen, boolean rightOpen) {
        if (len >= 5) {
            return 'W';
        }
        if (len == 4) {
            if (leftOpen && rightOpen) {
                return '4';
            }
            if (leftOpen || rightOpen) {
                return 'R';
            }
            return 'X';
        }
        if (len == 3) {
            if (leftOpen && rightOpen) {
                return '3';
            }
            if (leftOpen || rightOpen) {
                return 'S';
            }
            return 'X';
        }
        if (len == 2) {
            if (leftOpen && rightOpen) {
                return '2';
            }
            return 'X';
        }
        return 'N';
    }

    private static MoveAnalysis analyzeMovePattern(
            int[][] board, int size, int r, int c, int color) {
        if (r < 0 || r >= size || c < 0 || c >= size || board[r][c] != Stone.EMPTY) {
            return null;
        }
        board[r][c] = color;
        MoveAnalysis a = new MoveAnalysis();
        a.hasWin = WinChecker.checkWin(board, size, r, c, color);
        for (int[] dir : DIRS) {
            LineRunInfo info = getLineRunInfo(board, size, r, c, dir[0], dir[1], color);
            char seg = classifySegment(info.len, info.leftOpen, info.rightOpen);
            if (seg == '4') {
                a.nL4++;
            } else if (seg == 'R') {
                a.nR4++;
            } else if (seg == '3') {
                a.nL3++;
            } else if (seg == 'S') {
                a.nS3++;
            } else if (seg == '2') {
                a.nL2++;
            }
        }
        a.doubleRushFour = a.nR4 >= 2;
        a.independentDoubleRushFour = a.nR4 >= 2 && areIndependentRushFours(board, size, r, c, color);
        a.doubleLiveThree = a.nL3 >= 2;
        a.liveThreeAndRushFour = a.nL3 >= 1 && a.nR4 >= 1;
        a.doubleLiveTwo = a.nL2 >= 2;
        board[r][c] = Stone.EMPTY;
        return a;
    }

    /** 冲四：下一手成五的空位（与 gomoku.js rushFourWinningCell 一致） */
    private static int[] rushFourWinningCell(
            int[][] board, int size, int r, int c, int dr, int dc, int color) {
        int lr = r;
        int lc = c;
        while (lr - dr >= 0
                && lr - dr < size
                && lc - dc >= 0
                && lc - dc < size
                && board[lr - dr][lc - dc] == color) {
            lr -= dr;
            lc -= dc;
        }
        int rr = r;
        int rc = c;
        while (rr + dr >= 0
                && rr + dr < size
                && rc + dc >= 0
                && rc + dc < size
                && board[rr + dr][rc + dc] == color) {
            rr += dr;
            rc += dc;
        }
        int leftE =
                (lr - dr >= 0 && lr - dr < size && lc - dc >= 0 && lc - dc < size)
                        ? board[lr - dr][lc - dc]
                        : -1;
        int rightE =
                (rr + dr >= 0 && rr + dr < size && rc + dc >= 0 && rc + dc < size)
                        ? board[rr + dr][rc + dc]
                        : -1;
        if (leftE == Stone.EMPTY && rightE != Stone.EMPTY) {
            return new int[] {lr - dr, lc - dc};
        }
        if (rightE == Stone.EMPTY && leftE != Stone.EMPTY) {
            return new int[] {rr + dr, rc + dc};
        }
        return null;
    }

    private static boolean areIndependentRushFours(
            int[][] board, int size, int r, int c, int color) {
        Set<String> keys = new HashSet<>();
        for (int[] dir : DIRS) {
            LineRunInfo info = getLineRunInfo(board, size, r, c, dir[0], dir[1], color);
            if (classifySegment(info.len, info.leftOpen, info.rightOpen) != 'R') {
                continue;
            }
            int[] w = rushFourWinningCell(board, size, r, c, dir[0], dir[1], color);
            if (w == null) {
                continue;
            }
            keys.add(w[0] + "," + w[1]);
        }
        return keys.size() >= 2;
    }

    private static double shapeThreatScore(MoveAnalysis a) {
        if (a == null) {
            return 0;
        }
        if (a.hasWin) {
            return 100_000;
        }
        if (a.independentDoubleRushFour) {
            return 12_000;
        }
        if (a.nL4 >= 1) {
            return 10_000;
        }
        double v = a.nR4 * 1000.0 + a.nL3 * 100.0 + a.nS3 * 25.0 + a.nL2 * 5.0;
        boolean doubleThreat =
                (a.doubleLiveThree || a.liveThreeAndRushFour) && a.nL4 < 1;
        if (doubleThreat) {
            double bump = a.liveThreeAndRushFour ? 1500.0 : 500.0;
            if (a.doubleLiveThree && a.liveThreeAndRushFour) {
                bump = 1500.0;
            }
            v = Math.max(v, bump);
            v *= DOUBLE_THREAT_ATTACK_MULT;
        } else if (a.doubleLiveTwo && a.nL4 < 1) {
            v = Math.max(v, DOUBLE_LIVE_TWO_SHAPE_BUMP);
            double dm = Math.min(2.2, 1.0 + (DOUBLE_THREAT_ATTACK_MULT - 1) * 0.35);
            v *= dm;
        }
        return v;
    }

    /** 规则5：同分时天元/星位优先（与 gomoku.js centerTieBreakScore 一致） */
    private static double centerTieBreakScore(int r, int c, int size) {
        double mid = (size - 1) / 2.0;
        double man = Math.abs(r - mid) + Math.abs(c - mid);
        double cheb = Math.max(Math.abs(r - mid), Math.abs(c - mid));
        return -(man + 0.5 * cheb);
    }

    private static int[] pickBestByCenter(List<int[]> moves, int size) {
        if (moves == null || moves.isEmpty()) {
            return null;
        }
        int[] best = moves.get(0);
        double bestP = centerTieBreakScore(best[0], best[1], size);
        for (int i = 1; i < moves.size(); i++) {
            int[] m = moves.get(i);
            double p = centerTieBreakScore(m[0], m[1], size);
            if (p > bestP) {
                bestP = p;
                best = m;
            }
        }
        return best;
    }

    /**
     * 文档优先级 3–6：活四/双冲四、防对方活四/双冲四、双威胁攻防（在必胜/必堵五连之后）。
     */
    private static int[] findForcedPriorityMove(int[][] board, int size, int aiColor) {
        if (FORCED_TIERS_MAX < 3) {
            return null;
        }
        int opp = opposite(aiColor);
        List<int[]> tier3 = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] != Stone.EMPTY) {
                    continue;
                }
                MoveAnalysis a = analyzeMovePattern(board, size, r, c, aiColor);
                if (a == null || a.hasWin) {
                    continue;
                }
                if (a.nL4 >= 1 || a.independentDoubleRushFour) {
                    tier3.add(new int[] {r, c});
                }
            }
        }
        if (!tier3.isEmpty()) {
            return pickBestByCenter(tier3, size);
        }
        if (FORCED_TIERS_MAX < 4) {
            return null;
        }
        List<int[]> tier4 = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] != Stone.EMPTY) {
                    continue;
                }
                MoveAnalysis b = analyzeMovePattern(board, size, r, c, opp);
                if (b != null && (b.nL4 >= 1 || b.independentDoubleRushFour)) {
                    tier4.add(new int[] {r, c});
                }
            }
        }
        if (!tier4.isEmpty()) {
            return pickBestByCenter(tier4, size);
        }
        if (FORCED_TIERS_MAX < 5) {
            return null;
        }
        List<int[]> tier5 = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] != Stone.EMPTY) {
                    continue;
                }
                MoveAnalysis s = analyzeMovePattern(board, size, r, c, aiColor);
                if (s == null || s.hasWin) {
                    continue;
                }
                if (s.nL4 >= 1 || s.independentDoubleRushFour) {
                    continue;
                }
                if (s.doubleLiveThree || s.liveThreeAndRushFour) {
                    tier5.add(new int[] {r, c});
                }
            }
        }
        if (!tier5.isEmpty()) {
            return pickBestByCenter(tier5, size);
        }
        if (FORCED_TIERS_MAX < 6) {
            return null;
        }
        List<int[]> tier6 = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] != Stone.EMPTY) {
                    continue;
                }
                MoveAnalysis t = analyzeMovePattern(board, size, r, c, opp);
                if (t == null || t.nL4 >= 1 || t.independentDoubleRushFour) {
                    continue;
                }
                if (t.doubleLiveThree || t.liveThreeAndRushFour) {
                    tier6.add(new int[] {r, c});
                }
            }
        }
        if (!tier6.isEmpty()) {
            return pickBestByCenter(tier6, size);
        }
        if (FORCED_TIERS_MAX < 7) {
            return null;
        }
        List<int[]> tier7 = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] != Stone.EMPTY) {
                    continue;
                }
                MoveAnalysis u = analyzeMovePattern(board, size, r, c, opp);
                if (u == null || u.nL4 >= 1 || u.independentDoubleRushFour) {
                    continue;
                }
                if (u.doubleLiveThree || u.liveThreeAndRushFour) {
                    continue;
                }
                if (u.doubleLiveTwo) {
                    tier7.add(new int[] {r, c});
                }
            }
        }
        if (!tier7.isEmpty()) {
            return pickBestByCenter(tier7, size);
        }
        return null;
    }

    /** 假设在空位落子后的四向棋型分之和（用于候选排序） */
    private static int heuristicMoveScore(int[][] board, int size, int r, int c, int color) {
        if (r < 0 || r >= size || c < 0 || c >= size || board[r][c] != Stone.EMPTY) {
            return 0;
        }
        board[r][c] = color;
        int sum = 0;
        for (int[] dir : DIRS) {
            sum += linePatternScoreFromStone(board, size, r, c, dir[0], dir[1], color);
        }
        board[r][c] = Stone.EMPTY;
        return sum;
    }

    private static int linePatternScoreFromStone(
            int[][] board, int size, int r, int c, int dr, int dc, int color) {
        int len = 1;
        int nr = r - dr;
        int nc = c - dc;
        while (nr >= 0 && nr < size && nc >= 0 && nc < size && board[nr][nc] == color) {
            len++;
            nr -= dr;
            nc -= dc;
        }
        int leftEnd =
                (nr >= 0 && nr < size && nc >= 0 && nc < size) ? board[nr][nc] : -1;
        nr = r + dr;
        nc = c + dc;
        while (nr >= 0 && nr < size && nc >= 0 && nc < size && board[nr][nc] == color) {
            len++;
            nr += dr;
            nc += dc;
        }
        int rightEnd =
                (nr >= 0 && nr < size && nc >= 0 && nc < size) ? board[nr][nc] : -1;
        boolean leftOpen = (leftEnd == Stone.EMPTY);
        boolean rightOpen = (rightEnd == Stone.EMPTY);
        return patternMagnitude(len, leftOpen, rightOpen);
    }

    private static int[] findWinningMove(int[][] board, int size, int color) {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] != Stone.EMPTY) {
                    continue;
                }
                board[r][c] = color;
                boolean w = WinChecker.checkWin(board, size, r, c, color);
                board[r][c] = Stone.EMPTY;
                if (w) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    private static List<int[]> getCandidates(int[][] board, int size) {
        List<int[]> list = new ArrayList<>();
        boolean any = false;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] != Stone.EMPTY) {
                    any = true;
                    break;
                }
            }
            if (any) {
                break;
            }
        }
        if (!any) {
            list.add(new int[]{size / 2, size / 2});
            return list;
        }
        int ring = 2;
        boolean[][] seen = new boolean[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] == Stone.EMPTY) {
                    continue;
                }
                for (int dr = -ring; dr <= ring; dr++) {
                    for (int dc = -ring; dc <= ring; dc++) {
                        if (dr == 0 && dc == 0) {
                            continue;
                        }
                        int nr = r + dr;
                        int nc = c + dc;
                        if (nr >= 0 && nr < size && nc >= 0 && nc < size
                                && board[nr][nc] == Stone.EMPTY && !seen[nr][nc]) {
                            seen[nr][nc] = true;
                            list.add(new int[]{nr, nc});
                        }
                    }
                }
            }
        }
        if (list.isEmpty()) {
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    if (board[r][c] == Stone.EMPTY) {
                        list.add(new int[]{r, c});
                    }
                }
            }
            list.sort(Comparator.comparingDouble(m -> -centerTieBreakScore(m[0], m[1], size)));
        }
        return list;
    }

    private static int countStones(int[][] board, int size) {
        int n = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] != Stone.EMPTY) {
                    n++;
                }
            }
        }
        return n;
    }

    private static int opposite(int color) {
        return color == Stone.BLACK ? Stone.WHITE : Stone.BLACK;
    }
}
