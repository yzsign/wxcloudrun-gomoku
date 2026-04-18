package com.gomoku.sync.api.dto;

import java.util.List;

public class ShopCatalogResponse {

    private final List<ShopCatalogItemDto> items;

    public ShopCatalogResponse(List<ShopCatalogItemDto> items) {
        this.items = items;
    }

    public List<ShopCatalogItemDto> getItems() {
        return items;
    }
}
