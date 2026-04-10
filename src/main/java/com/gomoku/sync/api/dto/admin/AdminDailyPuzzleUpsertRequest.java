package com.gomoku.sync.api.dto.admin;

import com.gomoku.sync.api.dto.GameMoveDto;

import java.util.List;

/**
 * 管理端创建/更新每日残局题库。
 */
public class AdminDailyPuzzleUpsertRequest {

    private String title;
    private int difficulty = 1;
    private int boardSize = 15;
    /** 与线上一致：0 空 1 黑 2 白 */
    private int[][] board;
    /** 下一手：1 黑 2 白 */
    private int sideToMove = 1;
    private String goal = "WIN";
    private Integer maxUserMoves;
    private List<GameMoveDto> solutionMoves;
    private String hintText;
    /** 1 上架 0 下架 */
    private int status = 1;
    /**
     * 可选：创建/更新后把该日历日（yyyy-MM-dd，Asia/Shanghai）排期指向本题；
     * 为 null 时不改排期（更新时）或不绑定排期（创建时）。
     */
    private String scheduleDate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
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

    public int getSideToMove() {
        return sideToMove;
    }

    public void setSideToMove(int sideToMove) {
        this.sideToMove = sideToMove;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public Integer getMaxUserMoves() {
        return maxUserMoves;
    }

    public void setMaxUserMoves(Integer maxUserMoves) {
        this.maxUserMoves = maxUserMoves;
    }

    public List<GameMoveDto> getSolutionMoves() {
        return solutionMoves;
    }

    public void setSolutionMoves(List<GameMoveDto> solutionMoves) {
        this.solutionMoves = solutionMoves;
    }

    public String getHintText() {
        return hintText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(String scheduleDate) {
        this.scheduleDate = scheduleDate;
    }
}
