package com.gomoku.sync.api.dto;

public class SilentLoginResponse {

    private final long userId;

    public SilentLoginResponse(long userId) {
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
    }
}
