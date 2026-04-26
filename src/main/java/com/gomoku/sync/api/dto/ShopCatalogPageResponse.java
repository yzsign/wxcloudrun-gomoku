package com.gomoku.sync.api.dto;

import java.util.List;

/**
 * GET /api/me/shop/catalog 带 page 与 size 时：商品行、总条数、全量排序的 itemCode（与 DB sort_order 一致，供客户端选页与下标同步）。
 */
public class ShopCatalogPageResponse {

    private final List<ShopCatalogItemDto> items;
    private final int total;
    private final int page;
    private final int size;
    private final int totalPages;
    /** 所有上架商品 item_code 顺序（与全表 ORDER BY sort_order, id 一致） */
    private final List<String> orderItemCodes;

    public ShopCatalogPageResponse(
            List<ShopCatalogItemDto> items,
            int total,
            int page,
            int size,
            int totalPages,
            List<String> orderItemCodes) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
        this.orderItemCodes = orderItemCodes;
    }

    public List<ShopCatalogItemDto> getItems() {
        return items;
    }

    public int getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public List<String> getOrderItemCodes() {
        return orderItemCodes;
    }
}
