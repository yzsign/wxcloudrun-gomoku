package com.gomoku.sync.domain;

import org.springframework.web.socket.WebSocketSession;

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

    /**
     * @return 错误信息，null 表示成功
     */
    public String tryMove(int color, int r, int c) {
        lock.lock();
        try {
            if (gameOver) {
                return "对局已结束";
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
        } finally {
            lock.unlock();
        }
    }

    public ReentrantLock getLock() {
        return lock;
    }
}
