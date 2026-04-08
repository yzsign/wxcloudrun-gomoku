package com.gomoku.sync.api.dto;

/**
 * POST /api/me/piece-skin 成功响应。
 */
public class PieceSkinSelectResponse {

    private final String pieceSkinId;

    public PieceSkinSelectResponse(String pieceSkinId) {
        this.pieceSkinId = pieceSkinId;
    }

    public String getPieceSkinId() {
        return pieceSkinId;
    }
}
