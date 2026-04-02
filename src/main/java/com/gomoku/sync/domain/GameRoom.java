package com.gomoku.sync.domain;

import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 单房间状态：棋盘、轮次、双方 token 与连接
 */
public class GameRoom {

    private final String roomId;
    private final int size;
    private final String blackToken;
    private String whiteToken;
    private final int[][] board;
    private int current = Stone.BLACK;
    private boolean gameOver;
    private Integer winner;
    private WebSocketSession blackSession;
    private WebSocketSession whiteSession;
    private final ReentrantLock lock = new ReentrantLock();

    /** 走子顺序（用于悔棋） */
    private final Deque<RecordedMove> moveHistory = new ArrayDeque<>();

    /** 非空表示有待对方处理的悔棋申请（申请方为上一手行棋方） */
    private Integer pendingUndoRequesterColor;

    public GameRoom(String roomId, int size, String blackToken) {
        this.roomId = roomId;
        this.size = size;
        this.blackToken = blackToken;
        this.board = new int[size][size];
    }

    public String getRoomId() {
        return roomId;
    }

    public int getSize() {
        return size;
    }

    public String getBlackToken() {
        return blackToken;
    }

    public String getWhiteToken() {
        return whiteToken;
    }

    public void setWhiteToken(String whiteToken) {
        this.whiteToken = whiteToken;
    }

    public boolean hasGuest() {
        return whiteToken != null;
    }

    public int[][] getBoardCopy() {
        lock.lock();
        try {
            int[][] copy = new int[size][size];
            for (int i = 0; i < size; i++) {
                System.arraycopy(board[i], 0, copy[i], 0, size);
            }
            return copy;
        } finally {
            lock.unlock();
        }
    }

    public int getCurrent() {
        lock.lock();
        try {
            return current;
        } finally {
            lock.unlock();
        }
    }

    public boolean isGameOver() {
        lock.lock();
        try {
            return gameOver;
        } finally {
            lock.unlock();
        }
    }

    public Integer getWinner() {
        lock.lock();
        try {
            return winner;
        } finally {
            lock.unlock();
        }
    }

    public WebSocketSession getBlackSession() {
        return blackSession;
    }

    public WebSocketSession getWhiteSession() {
        return whiteSession;
    }

    public void setBlackSession(WebSocketSession blackSession) {
        this.blackSession = blackSession;
    }

    public void setWhiteSession(WebSocketSession whiteSession) {
        this.whiteSession = whiteSession;
    }

    public Integer resolveColorByToken(String token) {
        if (blackToken.equals(token)) {
            return Stone.BLACK;
        }
        if (whiteToken != null && whiteToken.equals(token)) {
            return Stone.WHITE;
        }
        return null;
    }

    public boolean isUndoPending() {
        lock.lock();
        try {
            return pendingUndoRequesterColor != null;
        } finally {
            lock.unlock();
        }
    }

    public Integer getPendingUndoRequesterColor() {
        lock.lock();
        try {
            return pendingUndoRequesterColor;
        } finally {
            lock.unlock();
        }
    }

    private static int oppositeColor(int color) {
        return color == Stone.BLACK ? Stone.WHITE : Stone.BLACK;
    }

    private int countStones() {
        int n = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] != Stone.EMPTY) {
                    n++;
                }
            }
        }
        return n;
    }

    private void syncCurrentFromBoard() {
        int n = countStones();
        if (n == 0) {
            current = Stone.BLACK;
        } else {
            current = (n % 2 == 1) ? Stone.WHITE : Stone.BLACK;
        }
    }

    /**
     * 申请悔棋：仅「上一手行棋方」（即当前轮到对方下时的一方）可发起。
     *
     * @return 错误信息，null 表示成功
     */
    public String requestUndo(int color) {
        lock.lock();
        try {
            if (gameOver) {
                return "对局已结束";
            }
            if (pendingUndoRequesterColor != null) {
                return "已有悔棋申请";
            }
            if (moveHistory.isEmpty()) {
                return "没有可悔的棋";
            }
            if (color != oppositeColor(current)) {
                return "只有上一手行棋方可申请悔棋";
            }
            pendingUndoRequesterColor = color;
            return null;
        } finally {
            lock.unlock();
        }
    }

    public String acceptUndo(int color) {
        lock.lock();
        try {
            if (pendingUndoRequesterColor == null) {
                return "没有待处理的悔棋";
            }
            if (pendingUndoRequesterColor == color) {
                return "须由对方同意";
            }
            if (color != current) {
                return "须由对方回应";
            }
            applyUndoPops();
            pendingUndoRequesterColor = null;
            return null;
        } finally {
            lock.unlock();
        }
    }

    public String rejectUndo(int color) {
        lock.lock();
        try {
            if (pendingUndoRequesterColor == null) {
                return "没有待处理的悔棋";
            }
            if (pendingUndoRequesterColor == color) {
                return "须由对方回应";
            }
            if (color != current) {
                return "须由对方回应";
            }
            pendingUndoRequesterColor = null;
            return null;
        } finally {
            lock.unlock();
        }
    }

    /** 申请人撤回悔棋申请 */
    public String cancelUndoRequest(int color) {
        lock.lock();
        try {
            if (pendingUndoRequesterColor == null) {
                return "没有悔棋申请";
            }
            if (pendingUndoRequesterColor != color) {
                return "仅申请人可取消";
            }
            pendingUndoRequesterColor = null;
            return null;
        } finally {
            lock.unlock();
        }
    }

    private void applyUndoPops() {
        if (moveHistory.isEmpty()) {
            return;
        }
        int pops = moveHistory.size() >= 2 ? 2 : 1;
        for (int i = 0; i < pops && !moveHistory.isEmpty(); i++) {
            RecordedMove m = moveHistory.removeLast();
            board[m.r][m.c] = Stone.EMPTY;
        }
        gameOver = false;
        winner = null;
        syncCurrentFromBoard();
    }

    /**
     * @return 错误信息，null 表示成功
     */
    public String tryMove(int color, int r, int c) {
        lock.lock();
        try {
            if (gameOver) {
                return "对局已结束";
            }
            if (pendingUndoRequesterColor != null) {
                return "请先处理悔棋申请";
            }
            if (current != color) {
                return "当前不是你的回合";
            }
            if (r < 0 || r >= size || c < 0 || c >= size) {
                return "坐标越界";
            }
            if (board[r][c] != Stone.EMPTY) {
                return "该位置已有棋子";
            }
            board[r][c] = color;
            moveHistory.addLast(new RecordedMove(r, c, color));
            if (WinChecker.checkWin(board, size, r, c, color)) {
                gameOver = true;
                winner = color;
            } else if (WinChecker.boardFull(board, size)) {
                gameOver = true;
                winner = null;
            } else {
                current = color == Stone.BLACK ? Stone.WHITE : Stone.BLACK;
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public void resetMatch() {
        lock.lock();
        try {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    board[i][j] = Stone.EMPTY;
                }
            }
            current = Stone.BLACK;
            gameOver = false;
            winner = null;
            moveHistory.clear();
            pendingUndoRequesterColor = null;
        } finally {
            lock.unlock();
        }
    }

    public static final class RecordedMove {
        public final int r;
        public final int c;
        public final int color;

        public RecordedMove(int r, int c, int color) {
            this.r = r;
            this.c = c;
            this.color = color;
        }
    }

    public ReentrantLock getLock() {
        return lock;
    }
}
