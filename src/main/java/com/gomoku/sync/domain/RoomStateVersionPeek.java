package com.gomoku.sync.domain;

/**
 * 批量轮询 room_game_state 时仅需房间号与版本（不必读 LONGTEXT）。
 */
public class RoomStateVersionPeek {

    private String roomId;
    private Long stateVersion;

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Long getStateVersion() {
        return stateVersion;
    }

    public void setStateVersion(Long stateVersion) {
        this.stateVersion = stateVersion;
    }
}
