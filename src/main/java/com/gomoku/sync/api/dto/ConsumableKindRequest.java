package com.gomoku.sync.api.dto;

/**
 * POST /api/me/consumables/redeem 与 /use 的请求体。
 */
public class ConsumableKindRequest {

    /** 如 dagger：短剑（Q）技能 */
    private String kind;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}
