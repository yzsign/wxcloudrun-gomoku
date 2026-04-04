package com.gomoku.sync.domain;

import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 单房间状态：棋盘、轮次、双方 token、真实用户 id 与连接
 */
public class GameRoom {

    private final String roomId;
    private final int size;
    private final String blackToken;
    private final long blackUserId;
    private String whiteToken;
    private Long whiteUserId;
    /** 随机匹配超时接入的数据库人机：无 WebSocket，由服务端代下白棋 */
    private boolean whiteIsBot;
    /** 白方为人机时：该人机账号在 DB 中配置的搜索深度区间（入座时写入；每步在区间内随机） */
    private int botSearchDepthMin = 2;
    private int botSearchDepthMax = 3;
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

    /** 同房间多局：首局为 1，每次「再来一局」RESET 后 +1，与 games.match_round、结算上报一致 */
    private int matchRound = 1;

    /**
     * 集群内该座位是否仍有连接（任一台实例上有 WebSocket 即 true，与 room_game_state 同步）。
     * 用于多实例下 STATE 中 blackConnected / whiteConnected。
     */
    private volatile boolean clusterBlackConnected;

    private volatile boolean clusterWhiteConnected;

    public GameRoom(String roomId, int size, String blackToken, long blackUserId) {
        this.roomId = roomId;
        this.size = size;
        this.blackToken = blackToken;
        this.blackUserId = blackUserId;
        this.board = new int[size][size];
    }

    public long getBlackUserId() {
        return blackUserId;
    }

    public Long getWhiteUserId() {
        lock.lock();
        try {
            return whiteUserId;
        } finally {
            lock.unlock();
        }
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

    public void setWhiteUserId(Long whiteUserId) {
        this.whiteUserId = whiteUserId;
    }

    public boolean isWhiteIsBot() {
        return whiteIsBot;
    }

    public void setWhiteIsBot(boolean whiteIsBot) {
        this.whiteIsBot = whiteIsBot;
    }

    /**
     * 人机入座时由匹配服务根据 users.bot_search_depth_* 设置；非人机可忽略。
     */
    public void setBotSearchDepthRange(int min, int max) {
        lock.lock();
        try {
            this.botSearchDepthMin = min;
            this.botSearchDepthMax = max;
        } finally {
            lock.unlock();
        }
    }

    public int getBotSearchDepthMin() {
        lock.lock();
        try {
            return botSearchDepthMin;
        } finally {
            lock.unlock();
        }
    }

    public int getBotSearchDepthMax() {
        lock.lock();
        try {
            return botSearchDepthMax;
        } finally {
            lock.unlock();
        }
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

    public int getMatchRound() {
        lock.lock();
        try {
            return matchRound;
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

    public boolean isClusterBlackConnected() {
        return clusterBlackConnected;
    }

    public void setClusterBlackConnected(boolean clusterBlackConnected) {
        this.clusterBlackConnected = clusterBlackConnected;
    }

    public boolean isClusterWhiteConnected() {
        return clusterWhiteConnected;
    }

    public void setClusterWhiteConnected(boolean clusterWhiteConnected) {
        this.clusterWhiteConnected = clusterWhiteConnected;
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

    /** 棋盘是否无子（新局未下或已 reset） */
    public boolean isBoardEmpty() {
        lock.lock();
        try {
            return countStones() == 0;
        } finally {
            lock.unlock();
        }
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
            if (moveHistory.isEmpty()) {
                return "没有可悔的棋";
            }
            RecordedMove last = moveHistory.peekLast();
            if (last.color != pendingUndoRequesterColor) {
                return "悔棋数据异常";
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

    /**
     * 同意悔棋时只撤回申请方上一手（与 pendingUndoRequesterColor 一致），不移除对手棋子。
     * 调用方须在持锁下保证 peekLast().color == pendingUndoRequesterColor。
     */
    private void applyUndoPops() {
        RecordedMove m = moveHistory.removeLast();
        board[m.r][m.c] = Stone.EMPTY;
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
            matchRound++;
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

    public GameRoomStateSnapshot toStateSnapshot() {
        lock.lock();
        try {
            GameRoomStateSnapshot s = new GameRoomStateSnapshot();
            s.setBoardSize(size);
            int[][] b = new int[size][size];
            for (int i = 0; i < size; i++) {
                System.arraycopy(board[i], 0, b[i], 0, size);
            }
            s.setBoard(b);
            s.setCurrent(current);
            s.setGameOver(gameOver);
            s.setWinner(winner);
            s.setMatchRound(matchRound);
            s.setPendingUndoRequesterColor(pendingUndoRequesterColor);
            s.setClusterBlackConnected(clusterBlackConnected);
            s.setClusterWhiteConnected(clusterWhiteConnected);
            List<GameRoomStateSnapshot.MoveRecord> list = new ArrayList<>();
            for (RecordedMove m : moveHistory) {
                list.add(new GameRoomStateSnapshot.MoveRecord(m.r, m.c, m.color));
            }
            s.setMoves(list);
            return s;
        } finally {
            lock.unlock();
        }
    }

    public void replaceGameStateFromSnapshot(GameRoomStateSnapshot snap) {
        if (snap == null || snap.getBoardSize() != size) {
            throw new IllegalArgumentException("snapshot board size mismatch");
        }
        int[][] src = snap.getBoard();
        if (src == null || src.length != size) {
            throw new IllegalArgumentException("snapshot board invalid");
        }
        lock.lock();
        try {
            for (int i = 0; i < size; i++) {
                System.arraycopy(src[i], 0, board[i], 0, size);
            }
            current = snap.getCurrent();
            gameOver = snap.isGameOver();
            winner = snap.getWinner();
            matchRound = snap.getMatchRound();
            pendingUndoRequesterColor = snap.getPendingUndoRequesterColor();
            clusterBlackConnected = snap.isClusterBlackConnected();
            clusterWhiteConnected = snap.isClusterWhiteConnected();
            moveHistory.clear();
            if (snap.getMoves() != null) {
                for (GameRoomStateSnapshot.MoveRecord m : snap.getMoves()) {
                    moveHistory.addLast(new RecordedMove(m.r, m.c, m.color));
                }
            }
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
