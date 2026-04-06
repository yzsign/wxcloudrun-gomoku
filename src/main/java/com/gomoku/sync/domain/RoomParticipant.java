package com.gomoku.sync.domain;

/**
 * 房间与双方用户（DB 持久化，供结算校验）
 */
public class RoomParticipant {

    private String roomId;
    private long blackUserId;
    private String blackToken;
    private Long whiteUserId;
    private String whiteToken;
    private boolean whiteIsBot;
    private Integer botSearchDepthMin;
    private Integer botSearchDepthMax;
    /** 人机棋风，见 BotAiStyle.ordinal；NULL 表示由 white_user_id 推导 */
    private Integer botAiStyle;

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public long getBlackUserId() {
        return blackUserId;
    }

    public void setBlackUserId(long blackUserId) {
        this.blackUserId = blackUserId;
    }

    public String getBlackToken() {
        return blackToken;
    }

    public void setBlackToken(String blackToken) {
        this.blackToken = blackToken;
    }

    public Long getWhiteUserId() {
        return whiteUserId;
    }

    public void setWhiteUserId(Long whiteUserId) {
        this.whiteUserId = whiteUserId;
    }

    public String getWhiteToken() {
        return whiteToken;
    }

    public void setWhiteToken(String whiteToken) {
        this.whiteToken = whiteToken;
    }

    public boolean isWhiteIsBot() {
        return whiteIsBot;
    }

    public void setWhiteIsBot(boolean whiteIsBot) {
        this.whiteIsBot = whiteIsBot;
    }

    public Integer getBotSearchDepthMin() {
        return botSearchDepthMin;
    }

    public void setBotSearchDepthMin(Integer botSearchDepthMin) {
        this.botSearchDepthMin = botSearchDepthMin;
    }

    public Integer getBotSearchDepthMax() {
        return botSearchDepthMax;
    }

    public void setBotSearchDepthMax(Integer botSearchDepthMax) {
        this.botSearchDepthMax = botSearchDepthMax;
    }

    public Integer getBotAiStyle() {
        return botAiStyle;
    }

    public void setBotAiStyle(Integer botAiStyle) {
        this.botAiStyle = botAiStyle;
    }
}
