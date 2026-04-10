package com.gomoku.sync.domain;

/**
 * 每日残局题库 daily_puzzle
 */
public class DailyPuzzle {

    public static final String GOAL_WIN = "WIN";
    public static final String GOAL_DRAW = "DRAW";

    public static final int STATUS_OFFLINE = 0;
    public static final int STATUS_ONLINE = 1;

    private Long id;
    private String title;
    private int difficulty;
    private int boardSize;
    private String boardJson;
    private int sideToMove;
    private String goal;
    private Integer maxUserMoves;
    private String solutionMovesJson;
    private String hintText;
    private int status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getBoardJson() {
        return boardJson;
    }

    public void setBoardJson(String boardJson) {
        this.boardJson = boardJson;
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

    public String getSolutionMovesJson() {
        return solutionMovesJson;
    }

    public void setSolutionMovesJson(String solutionMovesJson) {
        this.solutionMovesJson = solutionMovesJson;
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
}
