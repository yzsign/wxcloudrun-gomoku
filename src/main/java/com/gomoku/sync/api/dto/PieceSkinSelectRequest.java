package com.gomoku.sync.api.dto;

/**
 * POST /api/me/piece-skin
 */
public class PieceSkinSelectRequest {

    private String pieceSkinId;

    public String getPieceSkinId() {
        return pieceSkinId;
    }

    public void setPieceSkinId(String pieceSkinId) {
        this.pieceSkinId = pieceSkinId;
    }
}
