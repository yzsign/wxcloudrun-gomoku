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
    /** 是否计入天梯；休闲好友房为 false */
    private final boolean ranked;

    public JoinRoomResponse(int boardSize, String yourToken, int yourColor, boolean ranked) {
        this.boardSize = boardSize;
        this.yourToken = yourToken;
        this.yourColor = yourColor;
        this.ranked = ranked;
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

    public boolean isRanked() {
        return ranked;
    }

    /** 兼容旧客户端：仅执白时非 null */
    public String getWhiteToken() {
        return yourColor == Stone.WHITE ? yourToken : null;
    }

    public String getBlackToken() {
        return yourColor == Stone.BLACK ? yourToken : null;
    }
}
