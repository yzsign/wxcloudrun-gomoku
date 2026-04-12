package com.gomoku.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomoku.sync.ai.BotAiStyle;
import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.domain.RoomParticipant;
import com.gomoku.sync.domain.Stone;
import com.gomoku.sync.mapper.RoomParticipantMapper;
import com.gomoku.sync.mapper.UserMapper;
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
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public RoomService(
            @Value("${gomoku.board-size:15}") int boardSize,
            RoomParticipantMapper roomParticipantMapper,
            RoomGameStateService roomGameStateService,
            ObjectMapper objectMapper,
            UserMapper userMapper) {
        this.boardSize = boardSize;
        this.roomParticipantMapper = roomParticipantMapper;
        this.roomGameStateService = roomGameStateService;
        this.objectMapper = objectMapper;
        this.userMapper = userMapper;
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
            room.setPuzzleTemplate(board, sideToMove);
            String boardJson = objectMapper.writeValueAsString(board);
            roomParticipantMapper.insertPuzzleBlack(
                    roomId,
                    creatorUserId,
                    blackToken,
                    creatorUserId,
                    spectatorToken,
                    boardJson,
                    sideToMove);
            roomGameStateService.insertPuzzleInitial(roomId, board, sideToMove);
            roomGameStateService.hydrateRoom(room);
            room.capturePuzzleFriendBaselineFromRoomState();
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
        }
        if (rp.isWhiteIsBot() || rp.isBlackIsBot()) {
            room.setWhiteIsBot(rp.isWhiteIsBot());
            room.setBlackIsBot(rp.isBlackIsBot());
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
                            : (rp.isWhiteIsBot() && rp.getWhiteUserId() != null
                                    ? BotAiStyle.forBotUserId(rp.getWhiteUserId()).ordinal()
                                    : BotAiStyle.randomOrdinal());
            room.setBotAiStyleOrdinal(ord);
        }
        if (rp.isPuzzleRoom()) {
            room.setPuzzleRoom(true);
            if (rp.getObserverToken() != null && !rp.getObserverToken().isEmpty()) {
                room.setSpectatorToken(rp.getObserverToken());
            }
            if (rp.getObserverUserId() != null) {
                room.setObserverUserId(rp.getObserverUserId());
            }
            if (rp.getPuzzleInitBoardJson() != null
                    && !rp.getPuzzleInitBoardJson().trim().isEmpty()
                    && rp.getPuzzleSideToMove() != null) {
                try {
                    int[][] tpl =
                            objectMapper.readValue(rp.getPuzzleInitBoardJson(), int[][].class);
                    DailyPuzzleAdminService.validateBoardCells(tpl, boardSize);
                    room.setPuzzleTemplate(tpl, rp.getPuzzleSideToMove());
                } catch (Exception ignored) {
                    // 无模板时好友进房无法启用人机，兼容旧数据
                }
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
     * 加入房间：普通房入座白方；残局好友房按棋谱「下一手」分配——下一手黑则好友执黑、人机执白，下一手白则好友执白、人机执黑。
     */
    public JoinResult joinRoom(String roomId, long guestUserId) {
        GameRoom room = getRoom(roomId);
        if (room == null) {
            return JoinResult.notFound();
        }
        synchronized (room) {
            if (room.hasGuest()) {
                return JoinResult.full();
            }
            if (room.getBlackUserId() == guestUserId) {
                return JoinResult.sameUser();
            }
            boolean puzzle =
                    room.isPuzzleRoom()
                            && room.hasPuzzleTemplate()
                            && room.getPuzzleTemplateSideToMove() != null;
            int stm =
                    puzzle ? room.getPuzzleTemplateSideToMove() : Stone.BLACK;

            if (puzzle && stm == Stone.BLACK) {
                Long botId = userMapper.selectRandomBotId();
                if (botId == null) {
                    return JoinResult.noBots();
                }
                long observerId = room.getObserverUserId();
                if (room.getBlackUserId() != observerId) {
                    return JoinResult.notFound();
                }
                String blackTok = UUID.randomUUID().toString();
                String whiteTok = UUID.randomUUID().toString();
                if (roomParticipantMapper.updatePuzzleJoinFriendAsBlack(
                                roomId,
                                observerId,
                                guestUserId,
                                blackTok,
                                botId,
                                whiteTok)
                        != 1) {
                    return JoinResult.notFound();
                }
                room.setBlackUserId(guestUserId);
                room.setBlackToken(blackTok);
                room.setWhiteUserId(botId);
                room.setWhiteToken(whiteTok);
                room.restoreLiveStateFromPuzzleTemplate(false);
                room.setWhiteIsBot(true);
                room.setBlackIsBot(false);
                applyPuzzleRoomBotMeta(room, roomId);
                return JoinResult.ok(blackTok, Stone.BLACK);
            }

            String whiteToken = UUID.randomUUID().toString();
            room.setWhiteToken(whiteToken);
            room.setWhiteUserId(guestUserId);
            room.setWhiteIsBot(false);
            if (roomParticipantMapper.updateWhite(roomId, guestUserId, whiteToken) != 1) {
                room.setWhiteToken(null);
                room.setWhiteUserId(null);
                return JoinResult.notFound();
            }
            if (puzzle && stm == Stone.WHITE) {
                room.restoreLiveStateFromPuzzleTemplate(false);
                room.setBlackIsBot(true);
                room.setWhiteIsBot(false);
                applyPuzzleRoomBotMeta(room, roomId);
            }
            return JoinResult.ok(whiteToken, Stone.WHITE);
        }
    }

    private void applyPuzzleRoomBotMeta(GameRoom room, String roomId) {
        int dmin = 3;
        int dmax = 4;
        int styleOrd = BotAiStyle.randomOrdinal();
        room.setBotSearchDepthRange(dmin, dmax);
        room.setBotAiStyleOrdinal(styleOrd);
        roomParticipantMapper.updatePuzzleRoomBots(
                roomId,
                room.isWhiteIsBot(),
                room.isBlackIsBot(),
                dmin,
                dmax,
                styleOrd);
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
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
            if (room.isWhiteIsBot() || room.isBlackIsBot()) {
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

    /**
     * 随机匹配超时接入白方人机后：50% 交换座位，使房主可能执白、人机执黑（棋盘须仍为空）。
     */
    public void maybeRandomSwapHumanBotSides(String roomId) {
        GameRoom room = getRoom(roomId);
        if (room == null) {
            return;
        }
        synchronized (room) {
            if (room.isPuzzleRoom()) {
                return;
            }
            if (!room.hasGuest() || !room.isBoardEmpty()) {
                return;
            }
            if (room.isWhiteIsBot() == room.isBlackIsBot()) {
                return;
            }
            if (!ThreadLocalRandom.current().nextBoolean()) {
                return;
            }
            try {
                room.swapBlackWhiteSeatsHumanVsBot();
                int n =
                        roomParticipantMapper.updateBothSides(
                                roomId,
                                room.getBlackUserId(),
                                room.getBlackToken(),
                                room.getWhiteUserId(),
                                room.getWhiteToken());
                if (n != 1) {
                    room.swapBlackWhiteSeatsHumanVsBot();
                    return;
                }
                roomParticipantMapper.updatePuzzleRoomBots(
                        roomId,
                        room.isWhiteIsBot(),
                        room.isBlackIsBot(),
                        room.getBotSearchDepthMin(),
                        room.getBotSearchDepthMax(),
                        room.getBotAiStyleOrdinal());
            } catch (Exception e) {
                try {
                    room.swapBlackWhiteSeatsHumanVsBot();
                } catch (Exception ignored) {
                    // ignored
                }
                throw new IllegalStateException("人机座位随机交换失败", e);
            }
            roomGameStateService.tryPersist(room);
        }
    }

    public static final class JoinResult {
        private final boolean ok;
        /** 加入方 WebSocket token */
        private final String guestToken;
        /** {@link Stone#BLACK} 或 {@link Stone#WHITE} */
        private final Integer guestColor;
        private final String error;

        private JoinResult(boolean ok, String guestToken, Integer guestColor, String error) {
            this.ok = ok;
            this.guestToken = guestToken;
            this.guestColor = guestColor;
            this.error = error;
        }

        public static JoinResult ok(String guestToken, int guestColor) {
            return new JoinResult(true, guestToken, guestColor, null);
        }

        public static JoinResult notFound() {
            return new JoinResult(false, null, null, "ROOM_NOT_FOUND");
        }

        public static JoinResult full() {
            return new JoinResult(false, null, null, "ROOM_FULL");
        }

        public static JoinResult sameUser() {
            return new JoinResult(false, null, null, "SAME_USER");
        }

        public static JoinResult noBots() {
            return new JoinResult(false, null, null, "NO_BOTS");
        }

        public boolean isOk() {
            return ok;
        }

        public String getGuestToken() {
            return guestToken;
        }

        public Integer getGuestColor() {
            return guestColor;
        }

        /** 兼容旧客户端：仅加入方为白时返回 token */
        public String getWhiteToken() {
            return ok && guestColor != null && guestColor == Stone.WHITE ? guestToken : null;
        }

        public String getBlackToken() {
            return ok && guestColor != null && guestColor == Stone.BLACK ? guestToken : null;
        }

        public String getError() {
            return error;
        }
    }
}
