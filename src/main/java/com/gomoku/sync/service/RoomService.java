package com.gomoku.sync.service;

import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.mapper.RoomParticipantMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {

    private final int boardSize;
    private final RoomParticipantMapper roomParticipantMapper;
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public RoomService(
            @Value("${gomoku.board-size:15}") int boardSize,
            RoomParticipantMapper roomParticipantMapper) {
        this.boardSize = boardSize;
        this.roomParticipantMapper = roomParticipantMapper;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public GameRoom createRoom(long blackUserId) {
        String roomId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String blackToken = UUID.randomUUID().toString();
        GameRoom room = new GameRoom(roomId, boardSize, blackToken, blackUserId);
        rooms.put(roomId, room);
        try {
            roomParticipantMapper.insertBlack(roomId, blackUserId);
        } catch (Exception e) {
            rooms.remove(roomId);
            throw new IllegalStateException("写入房间参与者失败", e);
        }
        return room;
    }

    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    /** 从内存中移除房间（仅用于匹配取消等场景） */
    public boolean removeRoomIfExists(String roomId) {
        roomParticipantMapper.deleteByRoomId(roomId);
        return rooms.remove(roomId) != null;
    }

    /**
     * 加入房间，生成白方 token；若房间不存在或已有白方则失败
     */
    public JoinResult joinRoom(String roomId, long whiteUserId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            return JoinResult.notFound();
        }
        synchronized (room) {
            if (room.hasGuest()) {
                return JoinResult.full();
            }
            if (room.getBlackUserId() == whiteUserId) {
                return JoinResult.sameUser();
            }
            String whiteToken = UUID.randomUUID().toString();
            room.setWhiteToken(whiteToken);
            room.setWhiteUserId(whiteUserId);
            if (roomParticipantMapper.updateWhite(roomId, whiteUserId) != 1) {
                room.setWhiteToken(null);
                room.setWhiteUserId(null);
                return JoinResult.notFound();
            }
            return JoinResult.ok(whiteToken);
        }
    }

    public static final class JoinResult {
        private final boolean ok;
        private final String whiteToken;
        private final String error;

        private JoinResult(boolean ok, String whiteToken, String error) {
            this.ok = ok;
            this.whiteToken = whiteToken;
            this.error = error;
        }

        public static JoinResult ok(String whiteToken) {
            return new JoinResult(true, whiteToken, null);
        }

        public static JoinResult notFound() {
            return new JoinResult(false, null, "ROOM_NOT_FOUND");
        }

        public static JoinResult full() {
            return new JoinResult(false, null, "ROOM_FULL");
        }

        public static JoinResult sameUser() {
            return new JoinResult(false, null, "SAME_USER");
        }

        public boolean isOk() {
            return ok;
        }

        public String getWhiteToken() {
            return whiteToken;
        }

        public String getError() {
            return error;
        }
    }
}
