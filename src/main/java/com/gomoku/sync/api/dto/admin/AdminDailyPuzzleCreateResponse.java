package com.gomoku.sync.api.dto.admin;

/** POST /api/admin/daily-puzzles */
public class AdminDailyPuzzleCreateResponse {

    private final long id;

    public AdminDailyPuzzleCreateResponse(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
