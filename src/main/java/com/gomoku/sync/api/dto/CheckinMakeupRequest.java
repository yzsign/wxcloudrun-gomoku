package com.gomoku.sync.api.dto;

/** POST /api/me/checkin/makeup 请求体：补签目标日 YYYY-MM-DD（Asia/Shanghai 日历日） */
public class CheckinMakeupRequest {

    private String ymd;

    public String getYmd() {
        return ymd;
    }

    public void setYmd(String ymd) {
        this.ymd = ymd;
    }
}
