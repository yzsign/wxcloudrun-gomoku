package com.gomoku.sync.api.dto;

import com.gomoku.sync.domain.Stone;

/**
 * POST /api/rooms/join 成功响应：加入方执子色与 WebSocket token。
 */
public class JoinRoomResponse {

    private final int boardSize;
    private final String yourToken;
    /** 与 Stone 一致：{@link Stone#BLACK}=1，{@link Stone#WHITE}=2 */
    private final int yourColor;

    public JoinRoomResponse(int boardSize, String yourToken, int yourColor) {
        this.boardSize = boardSize;
        this.yourToken = yourToken;
        this.yourColor = yourColor;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public String getYourToken() {
        return yourToken;
    }

    public int getYourColor() {
        return yourColor;
    }

    /** 兼容旧客户端：仅执白时非 null */
    public String getWhiteToken() {
        return yourColor == Stone.WHITE ? yourToken : null;
    }

    public String getBlackToken() {
        return yourColor == Stone.BLACK ? yourToken : null;
    }
}
