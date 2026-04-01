package com.gomoku.sync.service;

import com.gomoku.sync.domain.GameRoom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {

    private final int boardSize;
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public RoomService(@Value("${gomoku.board-size:15}") int boardSize) {
        this.boardSize = boardSize;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public GameRoom createRoom() {
        String roomId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String blackToken = UUID.randomUUID().toString();
        GameRoom room = new GameRoom(roomId, boardSize, blackToken);
        rooms.put(roomId, room);
        return room;
    }

    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    /**
     * 加入房间，生成白方 token；若房间不存在或已有白方则失败
     */
    public JoinResult joinRoom(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            return JoinResult.notFound();
        }
        synchronized (room) {
            if (room.hasGuest()) {
                return JoinResult.full();
            }
            String whiteToken = UUID.randomUUID().toString();
            room.setWhiteToken(whiteToken);
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
