package com.gomoku.sync.api.dto;

/**
 * POST /api/me/pve-game：本机人机终局归档（不计天梯分变化）。
 */
public class RecordPveGameRequest {

    /** 当前玩家是否执黑 */
    private boolean playBlack;
    /** 玩家视角：WIN | LOSS | DRAW */
    private String myResult;
    private int totalSteps;
    /** 可选，与 {@link com.gomoku.sync.domain.GameRecord#getMovesJson()} 一致 */
    private String movesJson;

    public boolean isPlayBlack() {
        return playBlack;
    }

    public void setPlayBlack(boolean playBlack) {
        this.playBlack = playBlack;
    }

    public String getMyResult() {
        return myResult;
    }

    public void setMyResult(String myResult) {
        this.myResult = myResult;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public String getMovesJson() {
        return movesJson;
    }

    public void setMovesJson(String movesJson) {
        this.movesJson = movesJson;
    }
}
