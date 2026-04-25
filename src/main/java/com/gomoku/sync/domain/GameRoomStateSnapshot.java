package com.gomoku.sync.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 可 JSON 序列化、持久化到 room_game_state 的对局快照（不含 token / 用户 id / WebSocket）。
 */
public class GameRoomStateSnapshot {

    public GameRoomStateSnapshot() {}

    private int boardSize;
    private int[][] board;
    private int current;
    private boolean gameOver;
    private Integer winner;
    private int matchRound;
    private List<MoveRecord> moves;
    private Integer pendingUndoRequesterColor;
    /** 0 无；1 或 2，与 pendingUndoRequesterColor 同时有效 */
    private int pendingUndoPops;
    /** 终局后再来一局邀请发起方颜色 */
    private Integer pendingRematchRequesterColor;
    /** 非空表示有待对方处理的和棋申请 */
    private Integer pendingDrawRequesterColor;
    /** 集群内任一端是否仍有黑方连接（跨实例展示用） */
    private boolean clusterBlackConnected;
    private boolean clusterWhiteConnected;

    /**
     * 当前行棋方须在此时刻前落子，否则判超时负（Unix 毫秒，墙钟）。
     * 0 表示未初始化（旧快照兼容时由内存侧补全）。
     */
    private long clockMoveDeadlineWallMs;
    /**
     * 本局总时限：自第一手起算，超过判和棋（Unix 毫秒）；0 表示尚未落子。
     */
    private long clockGameDeadlineWallMs;
    /** 非 0 表示因悔棋/和棋申请而暂停读秒，值为暂停开始时刻 */
    private long clockPauseStartedWallMs;
    /**
     * 好友房：双方真人均曾同时在线后不因单方断线再进入「等好友」读秒暂停（与 room 一致；旧快照无此字段时由盘面/手顺推断）。
     */
    private boolean friendBothHumanSeatsLiveOnce;
    /**
     * 终局原因：null 或空为普通终局；TIME_DRAW=总时限和棋；MOVE_TIMEOUT=当前行棋方思考超时负。
     */
    private String gameEndReason;

    public static GameRoomStateSnapshot empty(int boardSize) {
        GameRoomStateSnapshot s = new GameRoomStateSnapshot();
        s.boardSize = boardSize;
        s.board = new int[boardSize][boardSize];
        s.current = Stone.BLACK;
        s.gameOver = false;
        s.winner = null;
        s.matchRound = 1;
        s.moves = new ArrayList<>();
        s.pendingUndoRequesterColor = null;
        s.pendingUndoPops = 0;
        s.pendingRematchRequesterColor = null;
        s.pendingDrawRequesterColor = null;
        s.clusterBlackConnected = false;
        s.clusterWhiteConnected = false;
        s.clockMoveDeadlineWallMs = 0L;
        s.clockGameDeadlineWallMs = 0L;
        s.clockPauseStartedWallMs = 0L;
        s.friendBothHumanSeatsLiveOnce = false;
        s.gameEndReason = null;
        return s;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(int boardSize) {
        this.boardSize = boardSize;
    }

    public int[][] getBoard() {
        return board;
    }

    public void setBoard(int[][] board) {
        this.board = board;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public Integer getWinner() {
        return winner;
    }

    public void setWinner(Integer winner) {
        this.winner = winner;
    }

    public int getMatchRound() {
        return matchRound;
    }

    public void setMatchRound(int matchRound) {
        this.matchRound = matchRound;
    }

    public List<MoveRecord> getMoves() {
        return moves;
    }

    public void setMoves(List<MoveRecord> moves) {
        this.moves = moves;
    }

    public Integer getPendingUndoRequesterColor() {
        return pendingUndoRequesterColor;
    }

    public void setPendingUndoRequesterColor(Integer pendingUndoRequesterColor) {
        this.pendingUndoRequesterColor = pendingUndoRequesterColor;
    }

    public int getPendingUndoPops() {
        return pendingUndoPops;
    }

    public void setPendingUndoPops(int pendingUndoPops) {
        this.pendingUndoPops = pendingUndoPops;
    }

    public Integer getPendingRematchRequesterColor() {
        return pendingRematchRequesterColor;
    }

    public void setPendingRematchRequesterColor(Integer pendingRematchRequesterColor) {
        this.pendingRematchRequesterColor = pendingRematchRequesterColor;
    }

    public Integer getPendingDrawRequesterColor() {
        return pendingDrawRequesterColor;
    }

    public void setPendingDrawRequesterColor(Integer pendingDrawRequesterColor) {
        this.pendingDrawRequesterColor = pendingDrawRequesterColor;
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

    public long getClockMoveDeadlineWallMs() {
        return clockMoveDeadlineWallMs;
    }

    public void setClockMoveDeadlineWallMs(long clockMoveDeadlineWallMs) {
        this.clockMoveDeadlineWallMs = clockMoveDeadlineWallMs;
    }

    public long getClockGameDeadlineWallMs() {
        return clockGameDeadlineWallMs;
    }

    public void setClockGameDeadlineWallMs(long clockGameDeadlineWallMs) {
        this.clockGameDeadlineWallMs = clockGameDeadlineWallMs;
    }

    public long getClockPauseStartedWallMs() {
        return clockPauseStartedWallMs;
    }

    public void setClockPauseStartedWallMs(long clockPauseStartedWallMs) {
        this.clockPauseStartedWallMs = clockPauseStartedWallMs;
    }

    public boolean isFriendBothHumanSeatsLiveOnce() {
        return friendBothHumanSeatsLiveOnce;
    }

    public void setFriendBothHumanSeatsLiveOnce(boolean friendBothHumanSeatsLiveOnce) {
        this.friendBothHumanSeatsLiveOnce = friendBothHumanSeatsLiveOnce;
    }

    public String getGameEndReason() {
        return gameEndReason;
    }

    public void setGameEndReason(String gameEndReason) {
        this.gameEndReason = gameEndReason;
    }

    public static class MoveRecord {
        public int r;
        public int c;
        public int color;

        public MoveRecord() {}

        public MoveRecord(int r, int c, int color) {
            this.r = r;
            this.c = c;
            this.color = color;
        }
    }
}
