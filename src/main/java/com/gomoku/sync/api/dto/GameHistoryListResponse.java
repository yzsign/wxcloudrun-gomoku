package com.gomoku.sync.api.dto;

import java.util.List;

public class GameHistoryListResponse {

    private final List<GameHistoryItemResponse> items;
    private final boolean hasMore;

    public GameHistoryListResponse(List<GameHistoryItemResponse> items, boolean hasMore) {
        this.items = items;
        this.hasMore = hasMore;
    }

    public List<GameHistoryItemResponse> getItems() {
        return items;
    }

    public boolean isHasMore() {
        return hasMore;
    }
}
