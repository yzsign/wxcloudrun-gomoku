package com.gomoku.sync.api.dto;

public class EquipResponse {

    private final String category;
    private final String itemId;

    public EquipResponse(String category, String itemId) {
        this.category = category;
        this.itemId = itemId;
    }

    public String getCategory() {
        return category;
    }

    public String getItemId() {
        return itemId;
    }
}
