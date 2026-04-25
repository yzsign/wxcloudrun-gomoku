package com.gomoku.sync.service;

import com.gomoku.sync.domain.GameRoom;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 棋手在 Gomoku 房间 WebSocket 上的对局内状态（进程内，单实例部署）。
 * 与 {@link com.gomoku.sync.websocket.UserWebSocketRegistry}（用户推送链路的在线态）正交。
 * 以后可换 Redis 实现，保持方法签名稳定。
 */
@Component
public class GomokuPlayerPresenceRegistry {

    private final RoomService roomService;
    private final ConcurrentHashMap<Long, String> userIdToRoomId = new ConcurrentHashMap<>();
    /** 观战 Gomoku WS 已建连的 userId -> roomId（与棋手登记 {@link #userIdToRoomId} 互斥使用） */
    private final ConcurrentHashMap<Long, String> spectatorUserIdToRoomId = new ConcurrentHashMap<>();

    public GomokuPlayerPresenceRegistry(RoomService roomService) {
        this.roomService = roomService;
    }

    /** 棋手 WS 建连成功后登记 */
    public void registerPlaying(long userId, @NonNull String roomId) {
        if (userId <= 0 || roomId.isEmpty()) {
            return;
        }
        userIdToRoomId.put(userId, roomId);
    }

    /** 棋手 WS 关闭时清除 */
    public void unregister(long userId) {
        if (userId <= 0) {
            return;
        }
        userIdToRoomId.remove(userId);
    }

    /** 观战 WS 建连后登记，供好友列表「观战中」 */
    public void registerSpectating(long userId, @NonNull String roomId) {
        if (userId <= 0 || roomId.isEmpty()) {
            return;
        }
        spectatorUserIdToRoomId.put(userId, roomId);
    }

    public void unregisterSpectating(long userId) {
        if (userId <= 0) {
            return;
        }
        spectatorUserIdToRoomId.remove(userId);
    }

    /**
     * 好友列表：对方是否以观战身份挂在本进程某未终局房间的 Gomoku WS 上。
     * 房间终局/释放时清脏并返回 false。
     */
    public boolean isPeerSpectating(long peerUserId) {
        if (peerUserId <= 0) {
            return false;
        }
        String roomId = spectatorUserIdToRoomId.get(peerUserId);
        if (roomId == null) {
            return false;
        }
        GameRoom room = roomService.getRoom(roomId);
        if (room == null) {
            spectatorUserIdToRoomId.remove(peerUserId);
            return false;
        }
        if (room.isGameOver()) {
            spectatorUserIdToRoomId.remove(peerUserId);
            return false;
        }
        if (!room.getSpectatorUserIds().contains(peerUserId)) {
            spectatorUserIdToRoomId.remove(peerUserId);
            return false;
        }
        return true;
    }

    /**
     * 供好友列表：对方仍在登记的房间内、且对局未结束、且仍在该房执黑/白。
     * 已终局或房间已释放时清掉脏登记并返回 false。
     */
    public boolean isPeerInActiveGame(long peerUserId) {
        return findActiveRoomIdForPlayer(peerUserId) != null;
    }

    /**
     * 好友观战：若该用户为某未终局房间内的黑/白棋手，返回房间 id，否则 null（并尽量清脏登记）。
     */
    public String findActiveRoomIdForPlayer(long peerUserId) {
        if (peerUserId <= 0) {
            return null;
        }
        String roomId = userIdToRoomId.get(peerUserId);
        if (roomId == null) {
            return null;
        }
        GameRoom room = roomService.getRoom(roomId);
        if (room == null) {
            userIdToRoomId.remove(peerUserId);
            return null;
        }
        if (room.isGameOver()) {
            userIdToRoomId.remove(peerUserId);
            return null;
        }
        long black = room.getBlackUserId();
        Long white = room.getWhiteUserId();
        if (peerUserId == black) {
            return roomId;
        }
        if (white != null && peerUserId == white) {
            return roomId;
        }
        userIdToRoomId.remove(peerUserId);
        return null;
    }
}
