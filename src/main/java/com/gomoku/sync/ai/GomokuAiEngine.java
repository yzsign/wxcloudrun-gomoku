package com.gomoku.sync.ai;

import com.gomoku.sync.domain.Stone;
import com.gomoku.sync.domain.WinChecker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 五子棋人机落子（与前端 gomoku.js 思路接近：必胜/必堵 + 候选裁剪 + minimax）。
 */
public final class GomokuAiEngine {

    private static final int[][] DIRS = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
    private static final int SEARCH_DEPTH = 4;
    private static final int MAX_CANDIDATES = 32;

    private GomokuAiEngine() {}

    /**
     * @return 长度 2 的数组 [r, c]，不应返回 null（极端情况落中心空位）
     */
    public static int[] chooseMove(int[][] board, int size, int aiColor) {
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

        cands.sort(Comparator.comparingInt(
                (int[] m) -> -(scorePoint(board, size, m[0], m[1], aiColor)
                        + scorePoint(board, size, m[0], m[1], opp))));

        if (cands.size() > MAX_CANDIDATES) {
            cands = cands.subList(0, MAX_CANDIDATES);
        }

        int bestR = cands.get(0)[0];
        int bestC = cands.get(0)[1];
        int bestScore = Integer.MIN_VALUE;
        for (int[] m : cands) {
            int r = m[0];
            int c = m[1];
            if (board[r][c] != Stone.EMPTY) {
                continue;
            }
            board[r][c] = aiColor;
            int sc;
            if (WinChecker.checkWin(board, size, r, c, aiColor)) {
                board[r][c] = Stone.EMPTY;
                return new int[]{r, c};
            }
            sc = minimax(board, size, SEARCH_DEPTH - 1, false, aiColor, opp, Integer.MIN_VALUE, Integer.MAX_VALUE);
            board[r][c] = Stone.EMPTY;
            if (sc > bestScore) {
                bestScore = sc;
                bestR = r;
                bestC = c;
            }
        }
        return new int[]{bestR, bestC};
    }

    private static int minimax(
            int[][] board,
            int size,
            int depth,
            boolean maximizing,
            int aiColor,
            int opp,
            int alpha,
            int beta) {
        if (depth == 0) {
            return evaluate(board, size, aiColor);
        }
        int turn = maximizing ? aiColor : opp;
        List<int[]> cands = getCandidates(board, size);
        if (cands.isEmpty()) {
            return evaluate(board, size, aiColor);
        }
        cands.sort(Comparator.comparingInt(
                (int[] m) -> -(scorePoint(board, size, m[0], m[1], aiColor)
                        + scorePoint(board, size, m[0], m[1], opp))));
        if (cands.size() > MAX_CANDIDATES) {
            cands = cands.subList(0, MAX_CANDIDATES);
        }

        if (maximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int[] m : cands) {
                int r = m[0];
                int c = m[1];
                if (board[r][c] != Stone.EMPTY) {
                    continue;
                }
                board[r][c] = turn;
                int ev;
                if (WinChecker.checkWin(board, size, r, c, turn)) {
                    ev = 2_000_000 - (SEARCH_DEPTH - depth);
                } else if (WinChecker.boardFull(board, size)) {
                    ev = 0;
                } else {
                    ev = minimax(board, size, depth - 1, false, aiColor, opp, alpha, beta);
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
            int r = m[0];
            int c = m[1];
            if (board[r][c] != Stone.EMPTY) {
                continue;
            }
            board[r][c] = turn;
            int ev;
            if (WinChecker.checkWin(board, size, r, c, turn)) {
                ev = -2_000_000 + (SEARCH_DEPTH - depth);
            } else if (WinChecker.boardFull(board, size)) {
                ev = 0;
            } else {
                ev = minimax(board, size, depth - 1, true, aiColor, opp, alpha, beta);
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

    private static int evaluate(int[][] board, int size, int aiColor) {
        int opp = opposite(aiColor);
        int s = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                for (int[] dir : DIRS) {
                    s += lineScore(board, size, r, c, dir[0], dir[1], aiColor);
                    s -= lineScore(board, size, r, c, dir[0], dir[1], opp);
                }
            }
        }
        return s;
    }

    /** 从 (r,c) 沿方向取 5 格窗口计分（与 WinChecker 五连检测一致） */
    private static int lineScore(int[][] board, int size, int r, int c, int dr, int dc, int color) {
        int[] cells = new int[5];
        for (int k = 0; k < 5; k++) {
            int rr = r + dr * k;
            int cc = c + dc * k;
            if (rr < 0 || rr >= size || cc < 0 || cc >= size) {
                return 0;
            }
            cells[k] = board[rr][cc];
        }
        int my = 0;
        int op = 0;
        int em = 0;
        int oc = opposite(color);
        for (int v : cells) {
            if (v == color) {
                my++;
            } else if (v == oc) {
                op++;
            } else {
                em++;
            }
        }
        if (op > 0 && my > 0) {
            return 0;
        }
        if (my == 5) {
            return 2600000;
        }
        if (my == 4 && em == 1) {
            return 210000;
        }
        if (my == 3 && em == 2) {
            return 10000;
        }
        if (my == 2 && em == 3) {
            return 480;
        }
        if (my == 1 && em == 4) {
            return 48;
        }
        return 0;
    }

    /** 候选点排序：汇总经过 (r,c) 的各 5 格窗口棋型分 */
    private static int scorePoint(int[][] board, int size, int r, int c, int color) {
        int sum = 0;
        for (int[] dir : DIRS) {
            for (int k = 0; k < 5; k++) {
                int sr = r - dir[0] * k;
                int sc = c - dir[1] * k;
                if (sr >= 0 && sr < size && sc >= 0 && sc < size) {
                    sum += lineScore(board, size, sr, sc, dir[0], dir[1], color);
                }
            }
        }
        return sum;
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
