package com.gomoku.sync.domain;

/**
 * room_game_state 表行（仅 Mapper 使用）
 */
public class RoomGameStateRow {

    private String roomId;
    private String stateJson;
    private long stateVersion;

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getStateJson() {
        return stateJson;
    }

    public void setStateJson(String stateJson) {
        this.stateJson = stateJson;
    }

    public long getStateVersion() {
        return stateVersion;
    }

    public void setStateVersion(long stateVersion) {
        this.stateVersion = stateVersion;
    }
}
