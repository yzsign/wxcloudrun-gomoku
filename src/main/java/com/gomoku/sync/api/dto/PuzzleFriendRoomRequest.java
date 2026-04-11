package com.gomoku.sync.api.dto;

/**
 * 创建残局好友房：当前盘面与下一手方（好友入座白方，房主旁观）。
 */
public class PuzzleFriendRoomRequest {

    private int[][] board;
    /** 1=黑 2=白，与 Stone 一致 */
    private int sideToMove;

    public int[][] getBoard() {
        return board;
    }

    public void setBoard(int[][] board) {
        this.board = board;
    }

    public int getSideToMove() {
        return sideToMove;
    }

    public void setSideToMove(int sideToMove) {
        this.sideToMove = sideToMove;
    }
}
