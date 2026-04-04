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
    /** 集群内任一端是否仍有黑方连接（跨实例展示用） */
    private boolean clusterBlackConnected;
    private boolean clusterWhiteConnected;

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
        s.clusterBlackConnected = false;
        s.clusterWhiteConnected = false;
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
