package com.gomoku.sync.api.dto;

import java.util.List;

public class LeaderboardListResponse {

    private final List<LeaderboardEntryDto> items;

    public LeaderboardListResponse(List<LeaderboardEntryDto> items) {
        this.items = items;
    }

    public List<LeaderboardEntryDto> getItems() {
        return items;
    }
}
