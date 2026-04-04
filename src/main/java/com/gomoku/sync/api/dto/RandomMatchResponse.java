package com.gomoku.sync.api.dto;

/**
 * 随机匹配：host 等待白方；guest 与队列中房间配对成功。
 * 先后手可能随机交换：guest 的 yourColor 表示本用户座位；host 在对手加入后应调用 GET /api/match/random/paired 获取最终 token。
 */
public class RandomMatchResponse {

    /** "host" | "guest" */
    private final String role;
    private final String roomId;
    private final String blackToken;
    private final String whiteToken;
    private final int boardSize;
    /** "BLACK" | "WHITE"，本响应对应登录用户的座位；guest 必有，host 为 null */
    private final String yourColor;

    public RandomMatchResponse(String role, String roomId, String blackToken, String whiteToken, int boardSize) {
        this(role, roomId, blackToken, whiteToken, boardSize, null);
    }

    public RandomMatchResponse(
            String role,
            String roomId,
            String blackToken,
            String whiteToken,
            int boardSize,
            String yourColor) {
        this.role = role;
        this.roomId = roomId;
        this.blackToken = blackToken;
        this.whiteToken = whiteToken;
        this.boardSize = boardSize;
        this.yourColor = yourColor;
    }

    public String getRole() {
        return role;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getBlackToken() {
        return blackToken;
    }

    public String getWhiteToken() {
        return whiteToken;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public String getYourColor() {
        return yourColor;
    }
}
