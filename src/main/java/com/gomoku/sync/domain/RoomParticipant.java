package com.gomoku.sync.domain;

/**
 * 房间与双方用户（DB 持久化，供结算校验）
 */
public class RoomParticipant {

    private String roomId;
    private long blackUserId;
    private Long whiteUserId;

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

    public Long getWhiteUserId() {
        return whiteUserId;
    }

    public void setWhiteUserId(Long whiteUserId) {
        this.whiteUserId = whiteUserId;
    }
}
