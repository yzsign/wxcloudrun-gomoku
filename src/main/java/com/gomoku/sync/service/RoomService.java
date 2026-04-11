package com.gomoku.sync.service;

import com.gomoku.sync.ai.BotAiStyle;
import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.domain.RoomParticipant;
import com.gomoku.sync.domain.Stone;
import com.gomoku.sync.mapper.RoomParticipantMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RoomService {

    private final int boardSize;
    private final RoomParticipantMapper roomParticipantMapper;
    private final RoomGameStateService roomGameStateService;
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public RoomService(
            @Value("${gomoku.board-size:15}") int boardSize,
            RoomParticipantMapper roomParticipantMapper,
            RoomGameStateService roomGameStateService) {
        this.boardSize = boardSize;
        this.roomParticipantMapper = roomParticipantMapper;
        this.roomGameStateService = roomGameStateService;
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
            roomParticipantMapper.insertBlack(roomId, blackUserId, blackToken);
            roomGameStateService.insertInitial(roomId);
        } catch (Exception e) {
            rooms.remove(roomId);
            throw new IllegalStateException("写入房间参与者失败", e);
        }
        return room;
    }

    /**
     * 残局好友房：房主与 DB 黑方为同一用户，房主使用 {@link GameRoom#getSpectatorToken()} 旁观；
     * 好友通过 {@link #joinRoom(String, long)} 入座白方。
     */
    public GameRoom createPuzzleFriendRoom(long creatorUserId, int[][] board, int sideToMove) {
        DailyPuzzleAdminService.validateBoardCells(board, boardSize);
        if (sideToMove != Stone.BLACK && sideToMove != Stone.WHITE) {
            throw new IllegalArgumentException("sideToMove 须为 1（黑）或 2（白）");
        }
        String roomId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String blackToken = UUID.randomUUID().toString();
        String spectatorToken = UUID.randomUUID().toString();
        GameRoom room = new GameRoom(roomId, boardSize, blackToken, creatorUserId);
        room.setPuzzleRoom(true);
        room.setObserverUserId(creatorUserId);
        room.setSpectatorToken(spectatorToken);
        rooms.put(roomId, room);
        try {
            roomParticipantMapper.insertPuzzleBlack(
                    roomId, creatorUserId, blackToken, creatorUserId, spectatorToken);
            roomGameStateService.insertPuzzleInitial(roomId, board, sideToMove);
            roomGameStateService.hydrateRoom(room);
        } catch (IllegalArgumentException e) {
            rooms.remove(roomId);
            throw e;
        } catch (Exception e) {
            rooms.remove(roomId);
            throw new IllegalStateException("创建残局好友房失败", e);
        }
        return room;
    }

    /**
     * 获取房间：内存未命中时从 MySQL 恢复元数据（多实例下 HTTP 与 WS 可能落在不同实例）。
     */
    public GameRoom getRoom(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            return room;
        }
        return loadRoomFromDatabase(roomId);
    }

    private GameRoom loadRoomFromDatabase(String roomId) {
        RoomParticipant rp = roomParticipantMapper.selectByRoomId(roomId);
        if (rp == null || rp.getBlackToken() == null || rp.getBlackToken().isEmpty()) {
            return null;
        }
        return rooms.computeIfAbsent(roomId, id -> buildRoomFromParticipant(rp));
    }

    private GameRoom buildRoomFromParticipant(RoomParticipant rp) {
        GameRoom room =
                new GameRoom(rp.getRoomId(), boardSize, rp.getBlackToken(), rp.getBlackUserId());
        if (rp.getWhiteToken() != null
                && !rp.getWhiteToken().isEmpty()
                && rp.getWhiteUserId() != null) {
            room.setWhiteToken(rp.getWhiteToken());
            room.setWhiteUserId(rp.getWhiteUserId());
            if (rp.isWhiteIsBot()) {
                room.setWhiteIsBot(true);
                int dmin = rp.getBotSearchDepthMin() != null ? rp.getBotSearchDepthMin() : 2;
                int dmax = rp.getBotSearchDepthMax() != null ? rp.getBotSearchDepthMax() : 3;
                if (dmin > dmax) {
                    int t = dmin;
                    dmin = dmax;
                    dmax = t;
                }
                room.setBotSearchDepthRange(dmin, dmax);
                Integer style = rp.getBotAiStyle();
                int ord =
                        style != null
                                ? style
                                : BotAiStyle.forBotUserId(rp.getWhiteUserId()).ordinal();
                room.setBotAiStyleOrdinal(ord);
            }
        }
        if (rp.isPuzzleRoom()) {
            room.setPuzzleRoom(true);
            if (rp.getObserverToken() != null && !rp.getObserverToken().isEmpty()) {
                room.setSpectatorToken(rp.getObserverToken());
            }
            if (rp.getObserverUserId() != null) {
                room.setObserverUserId(rp.getObserverUserId());
            }
        }
        roomGameStateService.hydrateRoom(room);
        return room;
    }

    /** 随机匹配人机入座后，将人机标记、搜索深度与棋风写入 DB，供其他实例加载 */
    public void persistWhiteBotMeta(String roomId, int dmin, int dmax, int botAiStyleOrdinal) {
        roomParticipantMapper.updateBotMeta(roomId, true, dmin, dmax, botAiStyleOrdinal);
    }

    /** 从内存中移除房间（仅用于匹配取消等场景） */
    public boolean removeRoomIfExists(String roomId) {
        roomGameStateService.deleteByRoomId(roomId);
        roomParticipantMapper.deleteByRoomId(roomId);
        return rooms.remove(roomId) != null;
    }

    /**
     * 加入房间，生成白方 token；若房间不存在或已有白方则失败
     */
    public JoinResult joinRoom(String roomId, long whiteUserId) {
        GameRoom room = getRoom(roomId);
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
            if (roomParticipantMapper.updateWhite(roomId, whiteUserId, whiteToken) != 1) {
                room.setWhiteToken(null);
                room.setWhiteUserId(null);
                return JoinResult.notFound();
            }
            return JoinResult.ok(whiteToken);
        }
    }

    /**
     * 真人双方已入座后 50% 交换先后手（仅棋盘仍为空时）；同步 DB 与内存。
     *
     * @return 是否发生了交换
     */
    public boolean maybeSwapRandomSides(String roomId) {
        GameRoom room = getRoom(roomId);
        if (room == null) {
            return false;
        }
        synchronized (room) {
            if (room.isPuzzleRoom()) {
                return false;
            }
            if (room.isWhiteIsBot()) {
                return false;
            }
            if (!room.hasGuest()) {
                return false;
            }
            if (!ThreadLocalRandom.current().nextBoolean()) {
                return false;
            }
            room.swapHumanSeats();
            try {
                Long w = room.getWhiteUserId();
                if (w == null) {
                    room.swapHumanSeats();
                    return false;
                }
                int n =
                        roomParticipantMapper.updateBothSides(
                                roomId,
                                room.getBlackUserId(),
                                room.getBlackToken(),
                                w,
                                room.getWhiteToken());
                if (n != 1) {
                    room.swapHumanSeats();
                    return false;
                }
            } catch (Exception e) {
                room.swapHumanSeats();
                throw new IllegalStateException("交换先后手失败", e);
            }
            roomGameStateService.tryPersist(room);
            return true;
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
