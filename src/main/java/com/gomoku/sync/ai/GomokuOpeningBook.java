package com.gomoku.sync.ai;

import com.gomoku.sync.domain.Stone;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 开局库（与前端 opening_book.js 对齐）：天元、RIF 白 2 八邻、黑 3 定式表。
 * 候选点随机选取，避免人机每局走相同开局。
 */
public final class GomokuOpeningBook {

    private static final int SIZE = 15;
    private static final int CENTER = 7;

    private static final int[][] BLACK3_DIRECT =
            new int[][] {
                {9, 7}, {10, 7}, {5, 7}, {6, 6}, {6, 8}, {9, 5}, {9, 9},
                {5, 5}, {5, 9}, {6, 9}, {8, 5}, {8, 9}, {4, 7}
            };
    private static final int[][] BLACK3_DIAG =
            new int[][] {
                {9, 8}, {8, 9}, {9, 9}, {10, 8}, {8, 10}, {10, 10}, {6, 6},
                {6, 8}, {8, 6}, {10, 6}, {6, 10}, {5, 7}, {7, 5}
            };

    private GomokuOpeningBook() {}

    /** @return {r,c} 或 null 表示不在开局库范围 */
    public static int[] getJosekiMove(int[][] board, int size, int aiColor) {
        if (size != SIZE) {
            return null;
        }
        int stones = countStones(board, size);
        if (stones == 0 && aiColor == Stone.BLACK) {
            return new int[] {CENTER, CENTER};
        }
        if (stones == 1 && aiColor == Stone.WHITE) {
            int[] black = findStone(board, size, Stone.BLACK);
            if (black != null) {
                return whiteSecondMove(board, size, black[0], black[1]);
            }
            return null;
        }
        if (stones == 2 && aiColor == Stone.BLACK) {
            List<int[]> blacks = collectColor(board, size, Stone.BLACK);
            List<int[]> whites = collectColor(board, size, Stone.WHITE);
            if (blacks.size() == 1 && whites.size() == 1) {
                return blackThirdMove(board, size, blacks.get(0), whites.get(0));
            }
        }
        return null;
    }

    private static int[] whiteSecondMove(int[][] board, int size, int br, int bc) {
        if (br != CENTER || bc != CENTER) {
            return null;
        }
        List<int[]> list = new ArrayList<>(8);
        int[][] deltas =
                new int[][] {
                    {-1, 0}, {1, 0}, {0, -1}, {0, 1},
                    {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
                };
        for (int[] d : deltas) {
            int r = br + d[0];
            int c = bc + d[1];
            if (inBounds(r, c, size) && board[r][c] == Stone.EMPTY) {
                list.add(new int[] {r, c});
            }
        }
        return pickRandom(list);
    }

    private static int[] blackThirdMove(int[][] board, int size, int[] b, int[] w) {
        if (!isAdjacentToCenter(w[0], w[1])) {
            return blackThirdFallback(board, size, b, w);
        }
        String kind = classifyWhiteKind(w[0], w[1]);
        int[][] table;
        int q;
        if ("diag".equals(kind)) {
            table = BLACK3_DIAG;
            q = quartersForDiagWhite(w[0], w[1]);
        } else if ("direct".equals(kind)) {
            table = BLACK3_DIRECT;
            q = quartersForDirectWhite(w[0], w[1]);
        } else {
            return blackThirdFallback(board, size, b, w);
        }
        if (q < 0) {
            return blackThirdFallback(board, size, b, w);
        }
        int[] fromTable = pickBlackThirdFromTable(board, size, table, q);
        if (fromTable != null) {
            return fromTable;
        }
        return blackThirdFallback(board, size, b, w);
    }

    private static int[] pickBlackThirdFromTable(
            int[][] board, int size, int[][] canon, int q) {
        List<int[]> valid = new ArrayList<>();
        for (int[] p : canon) {
            int[] abs = rotateAroundCenter(p[0], p[1], q);
            if (inBounds(abs[0], abs[1], size)
                    && board[abs[0]][abs[1]] == Stone.EMPTY
                    && inCenter5(abs[0], abs[1])) {
                valid.add(abs);
            }
        }
        return pickRandom(valid);
    }

    private static int[] blackThirdFallback(int[][] board, int size, int[] b, int[] w) {
        int[] mv = extendPastWhite(b, w);
        if (inBounds(mv[0], mv[1], size) && board[mv[0]][mv[1]] == Stone.EMPTY) {
            return mv;
        }
        if (!(Math.abs(w[0] - b[0]) <= 1 && Math.abs(w[1] - b[1]) <= 1)) {
            mv = reflectWhiteThroughCenter(w);
            if (inBounds(mv[0], mv[1], size) && board[mv[0]][mv[1]] == Stone.EMPTY) {
                return mv;
            }
        }
        int dr = w[0] - b[0];
        int dc = w[1] - b[1];
        if (dr == 0 && dc == 0) {
            return null;
        }
        int[][] cand =
                new int[][] {
                    {b[0] - dr, b[1] - dc},
                    {b[0] + dr * 2, b[1] + dc * 2},
                    {b[0] + dc, b[1] + dr},
                    {b[0] - dc, b[1] - dr}
                };
        for (int[] p : cand) {
            if (inBounds(p[0], p[1], size) && board[p[0]][p[1]] == Stone.EMPTY) {
                return p;
            }
        }
        return null;
    }

    private static int[] extendPastWhite(int[] b, int[] w) {
        return new int[] {2 * w[0] - b[0], 2 * w[1] - b[1]};
    }

    private static int[] reflectWhiteThroughCenter(int[] w) {
        return new int[] {2 * CENTER - w[0], 2 * CENTER - w[1]};
    }

    private static String classifyWhiteKind(int wr, int wc) {
        int dr = wr - CENTER;
        int dc = wc - CENTER;
        if (dr != 0 && dc != 0) {
            if (Math.abs(dr) == Math.abs(dc)) {
                return "diag";
            }
            return "mixed";
        }
        return "direct";
    }

    private static int quartersForDirectWhite(int wr, int wc) {
        for (int q = 0; q < 4; q++) {
            int[] p = rotateAroundCenter(8, 7, q);
            if (p[0] == wr && p[1] == wc) {
                return q;
            }
        }
        return -1;
    }

    private static int quartersForDiagWhite(int wr, int wc) {
        for (int q = 0; q < 4; q++) {
            int[] p = rotateAroundCenter(8, 8, q);
            if (p[0] == wr && p[1] == wc) {
                return q;
            }
        }
        return -1;
    }

    private static int[] rotateAroundCenter(int r, int c, int q) {
        int dr = r - CENTER;
        int dc = c - CENTER;
        for (int t = 0; t < q; t++) {
            int ndr = -dc;
            int ndc = dr;
            dr = ndr;
            dc = ndc;
        }
        return new int[] {CENTER + dr, CENTER + dc};
    }

    private static boolean isAdjacentToCenter(int r, int c) {
        return Math.abs(r - CENTER) <= 1
                && Math.abs(c - CENTER) <= 1
                && !(r == CENTER && c == CENTER);
    }

    private static boolean inCenter5(int r, int c) {
        return r >= 5 && r <= 9 && c >= 5 && c <= 9;
    }

    private static boolean inBounds(int r, int c, int size) {
        return r >= 0 && r < size && c >= 0 && c < size;
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

    private static int[] findStone(int[][] board, int size, int color) {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] == color) {
                    return new int[] {r, c};
                }
            }
        }
        return null;
    }

    private static List<int[]> collectColor(int[][] board, int size, int color) {
        List<int[]> out = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] == color) {
                    out.add(new int[] {r, c});
                }
            }
        }
        return out;
    }

    private static int[] pickRandom(List<int[]> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }
}
