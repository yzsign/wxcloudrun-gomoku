package com.gomoku.sync.ai;

import com.gomoku.sync.domain.Stone;
import com.gomoku.sync.domain.WinChecker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    /** 根节点候选上限 */
    private static final int MAX_CANDIDATES_ROOT = 18;
    /** 更深层的候选上限（减少分支因子） */
    private static final int MAX_CANDIDATES_DEEP = 12;
    /** 单步思考时间上限（纳秒），超时则叶节点提前返回局面分 */
    private static final long MOVE_TIME_BUDGET_NANOS = 70_000_000L;
    /** 实际搜索深度上限（与 DB 取 min） */
    private static final int EFFECTIVE_DEPTH_CAP = 6;
    /**
     * 对手线型威胁在评估中加权：强化对活二、活三、活四（及冲四等）的防守倾向，与
     * {@link #ORDER_OPP_BLOCK_WEIGHT} 候选排序配合。
     */
    private static final double OPP_LINE_PATTERN_MULT = 1.18;
    /** 候选排序：同点对方假想棋型分权重（与 gomoku.js sortMovesByHeuristic 一致） */
    private static final double ORDER_OPP_BLOCK_WEIGHT = 1.55;

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

        List<int[]> cands = getCandidates(board, size);
        if (cands.isEmpty()) {
            return new int[]{size / 2, size / 2};
        }

        sortCandidates(board, size, cands, aiColor, opp, st);

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
            int aiColor,
            int opp,
            BotAiStyle st) {
        double wm = st.getOrderMyW();
        double wo = st.getOrderOppW();
        cands.sort(
                Comparator.comparingDouble(
                        (int[] m) -> {
                            int r = m[0];
                            int c = m[1];
                            double h = heuristicMoveScore(board, size, r, c, aiColor);
                            double h2 = heuristicMoveScore(board, size, r, c, opp);
                            return -(wm * h + wo * (h2 * ORDER_OPP_BLOCK_WEIGHT));
                        }));
    }

    private static int maxCandForDepth(int depthRemaining) {
        if (depthRemaining >= 3) {
            return MAX_CANDIDATES_ROOT;
        }
        if (depthRemaining >= 2) {
            return MAX_CANDIDATES_DEEP;
        }
        return Math.min(10, MAX_CANDIDATES_DEEP);
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
        if (depth == 0) {
            return evaluateBoard(board, size, aiColor, st);
        }
        int turn = maximizing ? aiColor : opp;
        List<int[]> cands = getCandidates(board, size);
        if (cands.isEmpty()) {
            return evaluateBoard(board, size, aiColor, st);
        }
        sortCandidates(board, size, cands, aiColor, opp, st);
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
     * 与 gomoku.js linePatternScore 一致：连子长度与两端是否为空决定活二/活三/活四等权重。
     */
    private static int patternMagnitude(int len, boolean leftOpen, boolean rightOpen) {
        int openEnds = (leftOpen ? 1 : 0) + (rightOpen ? 1 : 0);
        if (len >= 5) {
            return 10_000_000;
        }
        if (len == 4) {
            if (openEnds == 2) {
                return 500_000;
            }
            if (openEnds == 1) {
                return 120_000;
            }
            return 8_000;
        }
        if (len == 3) {
            if (openEnds == 2) {
                return 45_000;
            }
            if (openEnds == 1) {
                return 6_000;
            }
            return 400;
        }
        if (len == 2) {
            if (openEnds == 2) {
                return 2_200;
            }
            if (openEnds == 1) {
                return 350;
            }
            return 40;
        }
        if (len == 1) {
            if (openEnds == 2) {
                return 120;
            }
            if (openEnds == 1) {
                return 25;
            }
            return 3;
        }
        return 0;
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
        int ring = 3;
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
