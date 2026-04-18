package com.gomoku.sync.api.dto;

/**
 * POST /api/me/equip：按种类装备唯一一件（PIECE_SKIN / THEME / BOARD_SKILL 等）。
 */
public class EquipRequest {

    /** {@link com.gomoku.sync.domain.CosmeticCategory} */
    private String category;
    private String itemId;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
}
