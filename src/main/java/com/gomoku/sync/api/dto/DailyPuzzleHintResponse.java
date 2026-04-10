package com.gomoku.sync.api.dto;

public class DailyPuzzleHintResponse {

    private final String hintText;

    public DailyPuzzleHintResponse(String hintText) {
        this.hintText = hintText;
    }

    public String getHintText() {
        return hintText;
    }
}
