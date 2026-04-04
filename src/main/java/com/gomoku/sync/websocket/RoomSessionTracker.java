package com.gomoku.sync.websocket;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本实例上仍有 WebSocket 连接的房间（用于轮询 room_game_state）。
 */
@Component
public class RoomSessionTracker {

    private final ConcurrentHashMap<String, Integer> refs = new ConcurrentHashMap<>();

    public void register(String roomId) {
        if (roomId == null || roomId.isEmpty()) {
            return;
        }
        refs.merge(roomId, 1, Integer::sum);
    }

    public void unregister(String roomId) {
        if (roomId == null || roomId.isEmpty()) {
            return;
        }
        refs.computeIfPresent(roomId, (k, v) -> v <= 1 ? null : v - 1);
    }

    public List<String> snapshotRoomIds() {
        return new ArrayList<>(refs.keySet());
    }
}
