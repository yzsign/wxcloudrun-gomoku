package com.gomoku.sync.api.dto;

/**
 * 随机匹配超时后接入数据库人机成功
 */
public class FallbackBotResponse {

    private final boolean paired;
    private final int boardSize;

    public FallbackBotResponse(boolean paired, int boardSize) {
        this.paired = paired;
        this.boardSize = boardSize;
    }

    public boolean isPaired() {
        return paired;
    }

    public int getBoardSize() {
        return boardSize;
    }
}
