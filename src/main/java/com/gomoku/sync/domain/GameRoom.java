package com.gomoku.sync.domain;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 单房间状态：棋盘、轮次、双方 token、真实用户 id 与连接
 */
public class GameRoom {

    /** 与 GomokuWebSocketHandler 中会话属性键一致，交换座位后需刷新 */
    private static final String WS_ATTR_COLOR = "color";

    private final String roomId;
    private final int size;
    /** 先手座位；随机匹配可能在双方入座后交换，与 white 侧对称可变 */
    private String blackToken;
    private long blackUserId;
    private String whiteToken;
    private Long whiteUserId;
    /** 随机匹配超时接入的数据库人机：无 WebSocket，由服务端代下白棋 */
    private boolean whiteIsBot;
    /** 白方为人机时：该人机账号在 DB 中配置的搜索深度区间（入座时写入；每步在区间内随机） */
    private int botSearchDepthMin = 2;
    private int botSearchDepthMax = 3;
    /** 白方为人机时：BotAiStyle 枚举 ordinal，与 room_participants.bot_ai_style 一致 */
    private int botAiStyleOrdinal;
    private final int[][] board;
    private int current = Stone.BLACK;
    private boolean gameOver;
    private Integer winner;
    private WebSocketSession blackSession;
    private WebSocketSession whiteSession;
    private final ReentrantLock lock = new ReentrantLock();

    /** 走子顺序（用于悔棋） */
    private final Deque<RecordedMove> moveHistory = new ArrayDeque<>();

    /** 非空表示有待对方处理的悔棋申请 */
    private Integer pendingUndoRequesterColor;
    /** 非空表示有待对方处理的和棋申请 */
    private Integer pendingDrawRequesterColor;
    /** 非空表示终局后一方已发起「再来一局」，待对方同意（与 undo 互斥） */
    private Integer pendingRematchRequesterColor;
    /** 同意时撤销的步数：1 仅撤回申请方上一手；2 对方已应手后连撤两手（须对方同意） */
    private int pendingUndoPops;

    /** 同房间多局：首局为 1，每次「再来一局」RESET 后 +1，与 games.match_round、结算上报一致 */
    private int matchRound = 1;

    /** 联机：各方上次成功发起悔棋申请的时间（毫秒）；新局 resetMatch 时清零 */
    private static final long UNDO_REQUEST_COOLDOWN_MS = 10_000L;
    private long lastUndoRequestWallClockMsBlack;
    private long lastUndoRequestWallClockMsWhite;

    /** 随机匹配 / 好友对战：每步 30s，超时当前行棋方负；自第一手起全局 30min 和棋 */
    public static final long CLOCK_MOVE_MS = 30_000L;
    public static final long CLOCK_GAME_MS = 1_800_000L;
    public static final String END_REASON_TIME_DRAW = "TIME_DRAW";
    public static final String END_REASON_MOVE_TIMEOUT = "MOVE_TIMEOUT";

    private long clockMoveDeadlineWallMs;
    private long clockGameDeadlineWallMs;
    private long clockPauseStartedWallMs;
    /** 终局原因，仅时钟类终局写入 */
    private String gameEndReason;

    /**
     * 集群内该座位是否仍有连接（任一台实例上有 WebSocket 即 true，与 room_game_state 同步）。
     * 用于多实例下 STATE 中 blackConnected / whiteConnected。
     */
    private volatile boolean clusterBlackConnected;

    private volatile boolean clusterWhiteConnected;

    /** 残局好友房：房主用 observerToken 旁观；黑方行棋若无人连黑座则顺延读秒 */
    private boolean puzzleRoom;
    private String spectatorToken;
    private long observerUserId;
    private WebSocketSession spectatorSession;

    public GameRoom(String roomId, int size, String blackToken, long blackUserId) {
        this.roomId = roomId;
        this.size = size;
        this.blackToken = blackToken;
        this.blackUserId = blackUserId;
        this.board = new int[size][size];
        initClockForFreshBoard();
    }

    private void initClockForFreshBoard() {
        long now = System.currentTimeMillis();
        clockGameDeadlineWallMs = 0L;
        clockPauseStartedWallMs = 0L;
        gameEndReason = null;
        clockMoveDeadlineWallMs = now + CLOCK_MOVE_MS;
    }

    private void startClockPause() {
        if (clockPauseStartedWallMs == 0L) {
            clockPauseStartedWallMs = System.currentTimeMillis();
        }
    }

    private void endClockPauseExtendBothDeadlines() {
        long now = System.currentTimeMillis();
        if (clockPauseStartedWallMs > 0L) {
            long pd = now - clockPauseStartedWallMs;
            clockMoveDeadlineWallMs += pd;
            if (clockGameDeadlineWallMs > 0L) {
                clockGameDeadlineWallMs += pd;
            }
            clockPauseStartedWallMs = 0L;
        }
    }

    /** 悔棋同意：暂停期间只顺延全局时限，应手后轮到谁下谁得满 30s */
    private void endClockPauseBeforeUndoAccept() {
        long now = System.currentTimeMillis();
        if (clockPauseStartedWallMs > 0L) {
            long pd = now - clockPauseStartedWallMs;
            if (clockGameDeadlineWallMs > 0L) {
                clockGameDeadlineWallMs += pd;
            }
            clockPauseStartedWallMs = 0L;
        }
    }

    /**
     * 多实例下由快照恢复时补全旧数据；对局未结束且读秒未设置时给默认 30s。
     */
    private void ensureClockDeadlinesFromSnapshot() {
        if (gameOver) {
            return;
        }
        long now = System.currentTimeMillis();
        if (clockMoveDeadlineWallMs <= 0L) {
            clockMoveDeadlineWallMs = now + CLOCK_MOVE_MS;
        }
    }

    /**
     * 若已满足超时或全局和棋条件则终局并返回 true（须由调用方持久化并广播）。
     */
    public boolean applyClockTimeoutsIfDue() {
        lock.lock();
        try {
            if (gameOver) {
                return false;
            }
            if (clockPauseStartedWallMs > 0L) {
                return false;
            }
            long now = System.currentTimeMillis();
            if (clockGameDeadlineWallMs > 0L && now >= clockGameDeadlineWallMs) {
                gameOver = true;
                winner = null;
                gameEndReason = END_REASON_TIME_DRAW;
                return true;
            }
            if (clockMoveDeadlineWallMs > 0L && now >= clockMoveDeadlineWallMs) {
                if (puzzleRoom) {
                    boolean blackMissing =
                            current == Stone.BLACK
                                    && (blackSession == null || !blackSession.isOpen());
                    boolean whiteMissing =
                            current == Stone.WHITE
                                    && !whiteIsBot
                                    && (whiteSession == null || !whiteSession.isOpen());
                    if (blackMissing || whiteMissing) {
                        clockMoveDeadlineWallMs = now + CLOCK_MOVE_MS;
                        return false;
                    }
                }
                gameOver = true;
                winner = oppositeColor(current);
                gameEndReason = END_REASON_MOVE_TIMEOUT;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public long getClockMoveDeadlineWallMs() {
        lock.lock();
        try {
            return clockMoveDeadlineWallMs;
        } finally {
            lock.unlock();
        }
    }

    public long getClockGameDeadlineWallMs() {
        lock.lock();
        try {
            return clockGameDeadlineWallMs;
        } finally {
            lock.unlock();
        }
    }

    public boolean isClockPaused() {
        lock.lock();
        try {
            return clockPauseStartedWallMs > 0L;
        } finally {
            lock.unlock();
        }
    }

    public String getGameEndReason() {
        lock.lock();
        try {
            return gameEndReason;
        } finally {
            lock.unlock();
        }
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

    public void setBotAiStyleOrdinal(int ordinal) {
        lock.lock();
        try {
            this.botAiStyleOrdinal = ordinal;
        } finally {
            lock.unlock();
        }
    }

    public int getBotAiStyleOrdinal() {
        lock.lock();
        try {
            return botAiStyleOrdinal;
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

    /**
     * @return "BLACK" / "WHITE" 或 null（非本房间参与者）
     */
    public String resolveSideColorName(long userId) {
        if (blackUserId == userId) {
            return "BLACK";
        }
        if (whiteUserId != null && whiteUserId == userId) {
            return "WHITE";
        }
        return null;
    }

    /**
     * 真人双方已入座后交换先后手（黑/白座位与 token）；仅用于随机匹配，棋盘须仍为空。
     */
    public void swapHumanSeats() {
        lock.lock();
        try {
            if (whiteIsBot) {
                throw new IllegalStateException("人机局不可交换座位");
            }
            if (whiteToken == null || whiteUserId == null) {
                throw new IllegalStateException("白方未入座");
            }
            if (!isBoardEmpty()) {
                throw new IllegalStateException("已有落子，不可交换座位");
            }
            long oldBlackUid = blackUserId;
            String oldBlackTok = blackToken;
            long oldWhiteUid = whiteUserId;
            String oldWhiteTok = whiteToken;

            blackUserId = oldWhiteUid;
            blackToken = oldWhiteTok;
            whiteUserId = oldBlackUid;
            whiteToken = oldBlackTok;

            WebSocketSession sb = blackSession;
            blackSession = whiteSession;
            whiteSession = sb;

            boolean cb = clusterBlackConnected;
            clusterBlackConnected = clusterWhiteConnected;
            clusterWhiteConnected = cb;

            refreshWebSocketSeatColorsFromTokens();
            initClockForFreshBoard();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 交换后 URI 中 token 不变，但房间侧黑/白 token 已对调，须按当前 token 重算 seat 色，否则与
     * {@link com.gomoku.sync.websocket.GomokuWebSocketHandler} 建立连接时写入的 ATTR 不一致（例如房主先连 WS 再配对）。
     */
    private void refreshWebSocketSeatColorsFromTokens() {
        refreshSessionColor(blackSession);
        refreshSessionColor(whiteSession);
    }

    private void refreshSessionColor(WebSocketSession session) {
        if (session == null || !session.isOpen()) {
            return;
        }
        URI uri = session.getUri();
        if (uri == null) {
            return;
        }
        String token = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
        if (token == null || token.isEmpty()) {
            return;
        }
        Integer c = resolveColorByToken(token);
        if (c != null) {
            session.getAttributes().put(WS_ATTR_COLOR, c);
        }
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

    public boolean isDrawPending() {
        lock.lock();
        try {
            return pendingDrawRequesterColor != null;
        } finally {
            lock.unlock();
        }
    }

    public Integer getPendingDrawRequesterColor() {
        lock.lock();
        try {
            return pendingDrawRequesterColor;
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

    private static RecordedMove peekSecondLast(Deque<RecordedMove> moveHistory) {
        if (moveHistory.size() < 2) {
            return null;
        }
        Iterator<RecordedMove> it = moveHistory.descendingIterator();
        it.next();
        return it.next();
    }

    /**
     * 申请悔棋：
     * <ul>
     *   <li>一手：仅「上一手行棋方」（轮到对方下时）可发起，撤回自己上一手；</li>
     *   <li>两手：对方已应手、轮到自己下时，可申请连撤两手（己方上一手与对方应手），须对方同意。</li>
     * </ul>
     *
     * @return 错误信息，null 表示成功
     */
    public String requestUndo(int color) {
        lock.lock();
        try {
            if (gameOver) {
                return "对局已结束";
            }
            if (pendingDrawRequesterColor != null) {
                return "请先处理和棋申请";
            }
            if (pendingUndoRequesterColor != null) {
                return "已有悔棋申请";
            }
            long now = System.currentTimeMillis();
            if (color == Stone.BLACK) {
                if (lastUndoRequestWallClockMsBlack != 0
                        && now - lastUndoRequestWallClockMsBlack < UNDO_REQUEST_COOLDOWN_MS) {
                    return "10秒内不可再次发起悔棋";
                }
            } else {
                if (lastUndoRequestWallClockMsWhite != 0
                        && now - lastUndoRequestWallClockMsWhite < UNDO_REQUEST_COOLDOWN_MS) {
                    return "10秒内不可再次发起悔棋";
                }
            }
            if (moveHistory.isEmpty()) {
                return "没有可悔的棋";
            }
            RecordedMove last = moveHistory.peekLast();
            int pops;
            if (color == oppositeColor(current) && last.color == color) {
                pops = 1;
            } else if (color == current && moveHistory.size() >= 2) {
                RecordedMove secondLast = peekSecondLast(moveHistory);
                if (secondLast == null) {
                    return "没有可悔的棋";
                }
                if (last.color != oppositeColor(color) || secondLast.color != color) {
                    return "只有上一手行棋方可申请悔棋";
                }
                pops = 2;
            } else {
                return "只有上一手行棋方可申请悔棋";
            }
            pendingUndoRequesterColor = color;
            pendingUndoPops = pops;
            if (color == Stone.BLACK) {
                lastUndoRequestWallClockMsBlack = now;
            } else {
                lastUndoRequestWallClockMsWhite = now;
            }
            startClockPause();
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 提议和棋，待对方同意或拒绝。
     *
     * @return 错误信息，null 表示成功
     */
    public String requestDraw(int color) {
        lock.lock();
        try {
            if (gameOver) {
                return "对局已结束";
            }
            if (pendingUndoRequesterColor != null) {
                return "请先处理悔棋申请";
            }
            if (pendingDrawRequesterColor != null) {
                return "已有和棋申请";
            }
            pendingDrawRequesterColor = color;
            startClockPause();
            return null;
        } finally {
            lock.unlock();
        }
    }

    public String acceptDraw(int color) {
        lock.lock();
        try {
            if (pendingDrawRequesterColor == null) {
                return "没有待处理的和棋申请";
            }
            if (pendingDrawRequesterColor == color) {
                return "须由对方同意";
            }
            if (color != oppositeColor(pendingDrawRequesterColor)) {
                return "须由对方回应";
            }
            gameOver = true;
            winner = null;
            pendingDrawRequesterColor = null;
            clockPauseStartedWallMs = 0L;
            return null;
        } finally {
            lock.unlock();
        }
    }

    public String rejectDraw(int color) {
        lock.lock();
        try {
            if (pendingDrawRequesterColor == null) {
                return "没有待处理的和棋申请";
            }
            if (pendingDrawRequesterColor == color) {
                return "须由对方回应";
            }
            if (color != oppositeColor(pendingDrawRequesterColor)) {
                return "须由对方回应";
            }
            endClockPauseExtendBothDeadlines();
            pendingDrawRequesterColor = null;
            return null;
        } finally {
            lock.unlock();
        }
    }

    public String cancelDrawRequest(int color) {
        lock.lock();
        try {
            if (pendingDrawRequesterColor == null) {
                return "没有和棋申请";
            }
            if (pendingDrawRequesterColor != color) {
                return "仅申请人可取消";
            }
            endClockPauseExtendBothDeadlines();
            pendingDrawRequesterColor = null;
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
            if (color != oppositeColor(pendingUndoRequesterColor)) {
                return "须由对方回应";
            }
            if (moveHistory.isEmpty()) {
                return "没有可悔的棋";
            }
            int pops = pendingUndoPops;
            if (pops == 1) {
                RecordedMove last = moveHistory.peekLast();
                if (last.color != pendingUndoRequesterColor) {
                    return "悔棋数据异常";
                }
            } else if (pops == 2) {
                if (moveHistory.size() < 2) {
                    return "悔棋数据异常";
                }
                RecordedMove last = moveHistory.peekLast();
                RecordedMove secondLast = peekSecondLast(moveHistory);
                if (secondLast == null
                        || last.color != oppositeColor(pendingUndoRequesterColor)
                        || secondLast.color != pendingUndoRequesterColor) {
                    return "悔棋数据异常";
                }
            } else {
                return "悔棋数据异常";
            }
            endClockPauseBeforeUndoAccept();
            applyUndoPops(pops);
            pendingUndoRequesterColor = null;
            pendingUndoPops = 0;
            clockMoveDeadlineWallMs = System.currentTimeMillis() + CLOCK_MOVE_MS;
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
            if (color != oppositeColor(pendingUndoRequesterColor)) {
                return "须由对方回应";
            }
            endClockPauseExtendBothDeadlines();
            pendingUndoRequesterColor = null;
            pendingUndoPops = 0;
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
            endClockPauseExtendBothDeadlines();
            pendingUndoRequesterColor = null;
            pendingUndoPops = 0;
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 按步数从栈顶撤销：一手仅申请方上一子；两手为对方应手与申请方上一子（先撤对方再撤己方）。
     */
    private void applyUndoPops(int count) {
        for (int i = 0; i < count; i++) {
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
            if (pendingDrawRequesterColor != null) {
                return "请先处理和棋申请";
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
            long now = System.currentTimeMillis();
            if (moveHistory.size() == 1) {
                clockGameDeadlineWallMs = now + CLOCK_GAME_MS;
            }
            if (WinChecker.checkWin(board, size, r, c, color)) {
                gameOver = true;
                winner = color;
                gameEndReason = null;
            } else if (WinChecker.boardFull(board, size)) {
                gameOver = true;
                winner = null;
                gameEndReason = null;
            } else {
                current = color == Stone.BLACK ? Stone.WHITE : Stone.BLACK;
                clockMoveDeadlineWallMs = now + CLOCK_MOVE_MS;
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 认输：对手获胜，终局。
     *
     * @return 错误信息，null 表示成功
     */
    public String tryResign(int color) {
        lock.lock();
        try {
            if (gameOver) {
                return "对局已结束";
            }
            pendingUndoRequesterColor = null;
            pendingUndoPops = 0;
            pendingDrawRequesterColor = null;
            clockPauseStartedWallMs = 0L;
            gameOver = true;
            winner = oppositeColor(color);
            gameEndReason = null;
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
            pendingUndoPops = 0;
            pendingRematchRequesterColor = null;
            pendingDrawRequesterColor = null;
            lastUndoRequestWallClockMsBlack = 0;
            lastUndoRequestWallClockMsWhite = 0;
            initClockForFreshBoard();
        } finally {
            lock.unlock();
        }
    }

    public Integer getPendingRematchRequesterColor() {
        lock.lock();
        try {
            return pendingRematchRequesterColor;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 终局后请求再来一局；人机时由黑方发起则立即开局。
     *
     * @return 错误信息，null 表示成功
     */
    public String requestRematch(int color) {
        lock.lock();
        try {
            if (!gameOver) {
                return "对局未结束";
            }
            if (pendingUndoRequesterColor != null) {
                return "请先处理悔棋申请";
            }
            if (pendingDrawRequesterColor != null) {
                return "请先处理和棋申请";
            }
            if (whiteIsBot && color == Stone.BLACK) {
                resetMatch();
                return null;
            }
            if (pendingRematchRequesterColor != null
                    && !pendingRematchRequesterColor.equals(color)) {
                return "已有待处理的再来一局邀请";
            }
            if (pendingRematchRequesterColor != null) {
                return null;
            }
            pendingRematchRequesterColor = color;
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 受邀方同意再来一局。
     */
    public String acceptRematch(int color) {
        lock.lock();
        try {
            if (pendingRematchRequesterColor == null) {
                return "暂无再来一局邀请";
            }
            if (pendingRematchRequesterColor == color) {
                return "需由对方同意";
            }
            resetMatch();
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 受邀方拒绝。
     */
    public String declineRematch(int color) {
        lock.lock();
        try {
            if (pendingRematchRequesterColor == null) {
                return "暂无邀请";
            }
            if (pendingRematchRequesterColor == color) {
                return "不能拒绝自己的邀请";
            }
            pendingRematchRequesterColor = null;
            return null;
        } finally {
            lock.unlock();
        }
    }

    /** 发起方撤销邀请 */
    public String cancelRematchRequest(int color) {
        lock.lock();
        try {
            if (pendingRematchRequesterColor == null) {
                return null;
            }
            if (!pendingRematchRequesterColor.equals(color)) {
                return "只能撤销自己的邀请";
            }
            pendingRematchRequesterColor = null;
            return null;
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
            s.setPendingUndoPops(pendingUndoRequesterColor == null ? 0 : pendingUndoPops);
            s.setPendingRematchRequesterColor(pendingRematchRequesterColor);
            s.setPendingDrawRequesterColor(pendingDrawRequesterColor);
            s.setClusterBlackConnected(clusterBlackConnected);
            s.setClusterWhiteConnected(clusterWhiteConnected);
            s.setClockMoveDeadlineWallMs(clockMoveDeadlineWallMs);
            s.setClockGameDeadlineWallMs(clockGameDeadlineWallMs);
            s.setClockPauseStartedWallMs(clockPauseStartedWallMs);
            s.setGameEndReason(gameEndReason);
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
            int snapPops = snap.getPendingUndoPops();
            if (pendingUndoRequesterColor == null) {
                pendingUndoPops = 0;
            } else if (snapPops == 1 || snapPops == 2) {
                pendingUndoPops = snapPops;
            } else {
                pendingUndoPops = 1;
            }
            clusterBlackConnected = snap.isClusterBlackConnected();
            clusterWhiteConnected = snap.isClusterWhiteConnected();
            pendingRematchRequesterColor = snap.getPendingRematchRequesterColor();
            pendingDrawRequesterColor = snap.getPendingDrawRequesterColor();
            clockMoveDeadlineWallMs = snap.getClockMoveDeadlineWallMs();
            clockGameDeadlineWallMs = snap.getClockGameDeadlineWallMs();
            clockPauseStartedWallMs = snap.getClockPauseStartedWallMs();
            gameEndReason = snap.getGameEndReason();
            moveHistory.clear();
            if (snap.getMoves() != null) {
                for (GameRoomStateSnapshot.MoveRecord m : snap.getMoves()) {
                    moveHistory.addLast(new RecordedMove(m.r, m.c, m.color));
                }
            }
            ensureClockDeadlinesFromSnapshot();
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

    public boolean isPuzzleRoom() {
        return puzzleRoom;
    }

    public void setPuzzleRoom(boolean puzzleRoom) {
        this.puzzleRoom = puzzleRoom;
    }

    public String getSpectatorToken() {
        return spectatorToken;
    }

    public void setSpectatorToken(String spectatorToken) {
        this.spectatorToken = spectatorToken;
    }

    public long getObserverUserId() {
        return observerUserId;
    }

    public void setObserverUserId(long observerUserId) {
        this.observerUserId = observerUserId;
    }

    public WebSocketSession getSpectatorSession() {
        return spectatorSession;
    }

    public void setSpectatorSession(WebSocketSession spectatorSession) {
        this.spectatorSession = spectatorSession;
    }
}
