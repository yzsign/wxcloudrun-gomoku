package com.gomoku.sync.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gomoku.sync.ai.BotAiStyle;
import com.gomoku.sync.ai.GomokuAiEngine;
import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.domain.Stone;
import com.gomoku.sync.service.PieceSkinSelectionService;
import com.gomoku.sync.service.RoomGameStateService;
import com.gomoku.sync.service.RoomService;
import com.gomoku.sync.service.SessionJwtService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
public class GomokuWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTR_ROOM = "room";
    private static final String ATTR_COLOR = "color";
    private static final String ATTR_SPECTATOR = "spectator";

    private final RoomService roomService;
    private final RoomGameStateService roomGameStateService;
    private final RoomSessionTracker roomSessionTracker;
    private final ObjectMapper objectMapper;
    private final SessionJwtService sessionJwtService;
    private final PieceSkinSelectionService pieceSkinSelectionService;
    private final ScheduledExecutorService botScheduler;
    /** 人机落子延迟任务，同房间新调度会取消旧任务 */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> pendingBotMoves = new ConcurrentHashMap<>();
    /** 人机对悔棋请求的延迟应答 */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> pendingBotUndoResponses =
            new ConcurrentHashMap<>();
    /** 人机对和棋请求的延迟应答 */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> pendingBotDrawResponses =
            new ConcurrentHashMap<>();

    public GomokuWebSocketHandler(
            RoomService roomService,
            RoomGameStateService roomGameStateService,
            RoomSessionTracker roomSessionTracker,
            ObjectMapper objectMapper,
            SessionJwtService sessionJwtService,
            PieceSkinSelectionService pieceSkinSelectionService,
            ScheduledExecutorService gomokuBotScheduler) {
        this.roomService = roomService;
        this.roomGameStateService = roomGameStateService;
        this.roomSessionTracker = roomSessionTracker;
        this.objectMapper = objectMapper;
        this.sessionJwtService = sessionJwtService;
        this.pieceSkinSelectionService = pieceSkinSelectionService;
        this.botScheduler = gomokuBotScheduler;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        Map<String, List<String>> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        String roomId = first(params.get("roomId"));
        String token = first(params.get("token"));
        String sessionToken = first(params.get("sessionToken"));
        if (roomId == null || token == null) {
            sendError(session, "缺少 roomId 或 token");
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        if (sessionToken == null || sessionToken.isEmpty()) {
            sendError(session, "缺少 sessionToken");
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        Optional<Long> userId = sessionJwtService.parseUserId(sessionToken);
        if (!userId.isPresent()) {
            sendError(session, "会话无效或已过期，请重新登录");
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        GameRoom room = roomService.getRoom(roomId);
        if (room == null) {
            sendError(session, "房间不存在");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        boolean spectator =
                room.isPuzzleRoom()
                        && room.getSpectatorToken() != null
                        && room.getSpectatorToken().equals(token)
                        && userId.get() == room.getObserverUserId();
        if (spectator) {
            roomGameStateService.syncRoomFromDbIfBehind(room);
            session.getAttributes().put(ATTR_ROOM, room);
            session.getAttributes().put(ATTR_SPECTATOR, Boolean.TRUE);
            synchronized (room) {
                if (room.getSpectatorSession() != null && room.getSpectatorSession().isOpen()) {
                    sendError(session, "旁观已有连接");
                    session.close(CloseStatus.NOT_ACCEPTABLE);
                    return;
                }
                room.setSpectatorSession(session);
            }
            roomSessionTracker.register(roomId);
            roomGameStateService.syncRoomFromDbIfBehind(room);
            applyOnlineClockTimeouts(room);
            broadcastState(room);
            maybePlayBot(room);
            return;
        }

        Integer color = room.resolveColorByToken(token);
        if (color == null) {
            sendError(session, "token 无效");
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        long seatUserId = color == Stone.BLACK ? room.getBlackUserId() : room.getWhiteUserId() == null ? -1L : room.getWhiteUserId();
        if (seatUserId != userId.get()) {
            sendError(session, "用户与座位不匹配");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        roomGameStateService.syncRoomFromDbIfBehind(room);

        session.getAttributes().put(ATTR_ROOM, room);
        session.getAttributes().put(ATTR_COLOR, color);

        synchronized (room) {
            if (color == Stone.BLACK) {
                if (room.getBlackSession() != null && room.getBlackSession().isOpen()) {
                    sendError(session, "黑方已有连接");
                    session.close(CloseStatus.NOT_ACCEPTABLE);
                    return;
                }
                room.setBlackSession(session);
            } else {
                if (room.getWhiteSession() != null && room.getWhiteSession().isOpen()) {
                    sendError(session, "白方已有连接");
                    session.close(CloseStatus.NOT_ACCEPTABLE);
                    return;
                }
                room.setWhiteSession(session);
            }
        }

        if (color == Stone.BLACK) {
            room.setClusterBlackConnected(true);
        } else if (!room.isWhiteIsBot()) {
            room.setClusterWhiteConnected(true);
        }
        if (room.isPuzzleRoom()) {
            if (color == Stone.WHITE && !room.isWhiteIsBot()) {
                room.tryApplyPuzzleFriendJoinResetOnce();
            } else if (color == Stone.BLACK && !room.isBlackIsBot()) {
                room.tryApplyPuzzleFriendJoinResetOnce();
            }
        }
        roomGameStateService.tryPersist(room);

        roomSessionTracker.register(roomId);

        roomGameStateService.syncRoomFromDbIfBehind(room);
        applyOnlineClockTimeouts(room);
        broadcastState(room);
        maybePlayBot(room);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (Boolean.TRUE.equals(session.getAttributes().get(ATTR_SPECTATOR))) {
            return;
        }
        GameRoom room = (GameRoom) session.getAttributes().get(ATTR_ROOM);
        Integer color = (Integer) session.getAttributes().get(ATTR_COLOR);
        if (room == null || color == null) {
            return;
        }

        JsonNode root = objectMapper.readTree(message.getPayload());
        String type = root.path("type").asText("");
        if ("MOVE".equals(type)) {
            handleMove(session, room, color, root);
        } else if ("RESET".equals(type) || "REMATCH_REQUEST".equals(type)) {
            handleRematchRequest(session, room, color);
        } else if ("REMATCH_ACCEPT".equals(type)) {
            handleRematchAccept(session, room, color);
        } else if ("REMATCH_DECLINE".equals(type)) {
            handleRematchDecline(session, room, color);
        } else if ("REMATCH_CANCEL".equals(type)) {
            handleRematchCancel(session, room, color);
        } else if ("UNDO_REQUEST".equals(type)) {
            handleUndoRequest(session, room, color);
        } else if ("UNDO_ACCEPT".equals(type)) {
            handleUndoAccept(session, room, color);
        } else if ("UNDO_REJECT".equals(type)) {
            handleUndoReject(session, room, color);
        } else if ("UNDO_CANCEL".equals(type)) {
            handleUndoCancel(session, room, color);
        } else if ("RESIGN".equals(type)) {
            handleResign(session, room, color);
        } else if ("DRAW_REQUEST".equals(type)) {
            handleDrawRequest(session, room, color);
        } else if ("DRAW_ACCEPT".equals(type)) {
            handleDrawAccept(session, room, color);
        } else if ("DRAW_REJECT".equals(type)) {
            handleDrawReject(session, room, color);
        } else if ("DRAW_CANCEL".equals(type)) {
            handleDrawCancel(session, room, color);
        } else {
            sendToSession(session, error("未知消息类型: " + type));
        }
    }

    /** 调用前须已 {@link RoomGameStateService#syncRoomFromDbIfBehind(GameRoom)} */
    private boolean applyOnlineClockTimeouts(GameRoom room) {
        if (!room.applyClockTimeoutsIfDue()) {
            return false;
        }
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
        }
        return true;
    }

    private void handleMove(WebSocketSession session, GameRoom room, int color, JsonNode root) {
        roomGameStateService.syncRoomFromDbIfBehind(room);
        if (applyOnlineClockTimeouts(room)) {
            sendToSession(session, error("对局已结束"));
            broadcastState(room);
            return;
        }
        int r = root.path("r").asInt(-1);
        int c = root.path("c").asInt(-1);
        String err = room.tryMove(color, r, c);
        if (err != null) {
            sendToSession(session, error(err));
            return;
        }
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
            sendToSession(session, error("对局状态已同步，请重试"));
            broadcastState(room);
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    private void handleUndoRequest(WebSocketSession session, GameRoom room, int color) {
        roomGameStateService.syncRoomFromDbIfBehind(room);
        if (applyOnlineClockTimeouts(room)) {
            sendToSession(session, error("对局已结束"));
            broadcastState(room);
            return;
        }
        String err = room.requestUndo(color);
        if (err != null) {
            sendToSession(session, error(err));
            return;
        }
        if (room.isWhiteIsBot()
                && room.isUndoPending()
                && room.getPendingUndoRequesterColor() != null
                && room.getPendingUndoRequesterColor() == Stone.BLACK) {
            if (!roomGameStateService.tryPersist(room)) {
                roomGameStateService.forceReloadFromDb(room);
                sendToSession(session, error("对局状态已同步，请重试"));
                broadcastState(room);
                return;
            }
            broadcastState(room);
            scheduleBotUndoResponse(room);
            return;
        }
        if (room.isBlackIsBot()
                && room.isUndoPending()
                && room.getPendingUndoRequesterColor() != null
                && room.getPendingUndoRequesterColor() == Stone.WHITE) {
            if (!roomGameStateService.tryPersist(room)) {
                roomGameStateService.forceReloadFromDb(room);
                sendToSession(session, error("对局状态已同步，请重试"));
                broadcastState(room);
                return;
            }
            broadcastState(room);
            scheduleBlackBotUndoResponse(room);
            return;
        }
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
            sendToSession(session, error("对局状态已同步，请重试"));
            broadcastState(room);
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    private void handleUndoAccept(WebSocketSession session, GameRoom room, int color) {
        roomGameStateService.syncRoomFromDbIfBehind(room);
        if (applyOnlineClockTimeouts(room)) {
            sendToSession(session, error("对局已结束"));
            broadcastState(room);
            return;
        }
        String err = room.acceptUndo(color);
        if (err != null) {
            sendToSession(session, error(err));
            return;
        }
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
            sendToSession(session, error("对局状态已同步，请重试"));
            broadcastState(room);
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    private void handleUndoReject(WebSocketSession session, GameRoom room, int color) {
        roomGameStateService.syncRoomFromDbIfBehind(room);
        if (applyOnlineClockTimeouts(room)) {
            sendToSession(session, error("对局已结束"));
            broadcastState(room);
            return;
        }
        String err = room.rejectUndo(color);
        if (err != null) {
            sendToSession(session, error(err));
            return;
        }
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
            sendToSession(session, error("对局状态已同步，请重试"));
            broadcastState(room);
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    private void handleUndoCancel(WebSocketSession session, GameRoom room, int color) {
        roomGameStateService.syncRoomFromDbIfBehind(room);
        if (applyOnlineClockTimeouts(room)) {
            sendToSession(session, error("对局已结束"));
            broadcastState(room);
            return;
        }
        cancelPendingBotUndoResponse(room.getRoomId());
        String err = room.cancelUndoRequest(color);
        if (err != null) {
            sendToSession(session, error(err));
            return;
        }
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
            sendToSession(session, error("对局状态已同步，请重试"));
            broadcastState(room);
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    private void handleDrawRequest(WebSocketSession session, GameRoom room, int color) {
        roomGameStateService.syncRoomFromDbIfBehind(room);
        if (applyOnlineClockTimeouts(room)) {
            sendToSession(session, error("对局已结束"));
            broadcastState(room);
            return;
        }
        String err = room.requestDraw(color);
        if (err != null) {
            sendToSession(session, error(err));
            return;
        }
        if (room.isWhiteIsBot()
                && room.isDrawPending()
                && room.getPendingDrawRequesterColor() != null
                && room.getPendingDrawRequesterColor() == Stone.BLACK) {
            if (!roomGameStateService.tryPersist(room)) {
                roomGameStateService.forceReloadFromDb(room);
                sendToSession(session, error("对局状态已同步，请重试"));
                broadcastState(room);
                return;
            }
            broadcastState(room);
            scheduleBotDrawResponse(room);
            return;
        }
        if (room.isBlackIsBot()
                && room.isDrawPending()
                && room.getPendingDrawRequesterColor() != null
                && room.getPendingDrawRequesterColor() == Stone.WHITE) {
            if (!roomGameStateService.tryPersist(room)) {
                roomGameStateService.forceReloadFromDb(room);
                sendToSession(session, error("对局状态已同步，请重试"));
                broadcastState(room);
                return;
            }
            broadcastState(room);
            scheduleBlackBotDrawResponse(room);
            return;
        }
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
            sendToSession(session, error("对局状态已同步，请重试"));
            broadcastState(room);
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    private void handleDrawAccept(WebSocketSession session, GameRoom room, int color) {
        roomGameStateService.syncRoomFromDbIfBehind(room);
        if (applyOnlineClockTimeouts(room)) {
            sendToSession(session, error("对局已结束"));
            broadcastState(room);
            return;
        }
        cancelPendingBotDrawResponse(room.getRoomId());
        String err = room.acceptDraw(color);
        if (err != null) {
            sendToSession(session, error(err));
            return;
        }
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
            sendToSession(session, error("对局状态已同步，请重试"));
            broadcastState(room);
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    private void handleDrawReject(WebSocketSession session, GameRoom room, int color) {
        roomGameStateService.syncRoomFromDbIfBehind(room);
        if (applyOnlineClockTimeouts(room)) {
            sendToSession(session, error("对局已结束"));
            broadcastState(room);
            return;
        }
        cancelPendingBotDrawResponse(room.getRoomId());
        String err = room.rejectDraw(color);
        if (err != null) {
            sendToSession(session, error(err));
            return;
        }
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
            sendToSession(session, error("对局状态已同步，请重试"));
            broadcastState(room);
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    private void handleDrawCancel(WebSocketSession session, GameRoom room, int color) {
        roomGameStateService.syncRoomFromDbIfBehind(room);
        if (applyOnlineClockTimeouts(room)) {
            sendToSession(session, error("对局已结束"));
            broadcastState(room);
            return;
        }
        cancelPendingBotDrawResponse(room.getRoomId());
        String err = room.cancelDrawRequest(color);
        if (err != null) {
            sendToSession(session, error(err));
            return;
        }
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
            sendToSession(session, error("对局状态已同步，请重试"));
            broadcastState(room);
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    private void handleResign(WebSocketSession session, GameRoom room, int color) {
        roomGameStateService.syncRoomFromDbIfBehind(room);
        if (applyOnlineClockTimeouts(room)) {
            sendToSession(session, error("对局已结束"));
            broadcastState(room);
            return;
        }
        cancelPendingBotTasks(room.getRoomId());
        String err = room.tryResign(color);
        if (err != null) {
            sendToSession(session, error(err));
            return;
        }
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
            sendToSession(session, error("对局状态已同步，请重试"));
            broadcastState(room);
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    /** 终局后发起「再来一局」邀请（同房间；人机侧可立即开局） */
    private void handleRematchRequest(WebSocketSession session, GameRoom room, int color) throws Exception {
        roomGameStateService.syncRoomFromDbIfBehind(room);
        cancelPendingBotTasks(room.getRoomId());
        room.getLock().lock();
        try {
            if (!room.isGameOver()) {
                if (room.isBoardEmpty()) {
                    broadcastState(room);
                    return;
                }
                sendToSession(session, error("对局未结束，不能再来一局"));
                return;
            }
            String err = room.requestRematch(color);
            if (err != null) {
                sendToSession(session, error(err));
                return;
            }
        } finally {
            room.getLock().unlock();
        }
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
            sendToSession(session, error("对局状态已同步，请重试"));
            broadcastState(room);
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    private void handleRematchAccept(WebSocketSession session, GameRoom room, int color) throws Exception {
        roomGameStateService.syncRoomFromDbIfBehind(room);
        cancelPendingBotTasks(room.getRoomId());
        room.getLock().lock();
        try {
            String err = room.acceptRematch(color);
            if (err != null) {
                sendToSession(session, error(err));
                return;
            }
        } finally {
            room.getLock().unlock();
        }
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
            sendToSession(session, error("对局状态已同步，请重试"));
            broadcastState(room);
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    private void handleRematchDecline(WebSocketSession session, GameRoom room, int color) throws Exception {
        roomGameStateService.syncRoomFromDbIfBehind(room);
        Integer requester = room.getPendingRematchRequesterColor();
        room.getLock().lock();
        try {
            String err = room.declineRematch(color);
            if (err != null) {
                sendToSession(session, error(err));
                return;
            }
        } finally {
            room.getLock().unlock();
        }
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
            sendToSession(session, error("对局状态已同步，请重试"));
            broadcastState(room);
            return;
        }
        if (requester != null) {
            WebSocketSession req =
                    requester == Stone.BLACK ? room.getBlackSession() : room.getWhiteSession();
            sendToSession(req, rematchDeclinedNotice());
        }
        broadcastState(room);
    }

    private void handleRematchCancel(WebSocketSession session, GameRoom room, int color) throws Exception {
        roomGameStateService.syncRoomFromDbIfBehind(room);
        room.getLock().lock();
        try {
            String err = room.cancelRematchRequest(color);
            if (err != null) {
                sendToSession(session, error(err));
                return;
            }
        } finally {
            room.getLock().unlock();
        }
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
            sendToSession(session, error("对局状态已同步，请重试"));
            broadcastState(room);
            return;
        }
        broadcastState(room);
    }

    private TextMessage rematchDeclinedNotice() {
        try {
            ObjectNode n = objectMapper.createObjectNode();
            n.put("type", "REMATCH_DECLINED");
            return new TextMessage(objectMapper.writeValueAsString(n));
        } catch (Exception e) {
            return new TextMessage("{\"type\":\"REMATCH_DECLINED\"}");
        }
    }

    /**
     * 白方或黑方为人机时：轮到该色且非悔棋/和棋待定时代为落子；随机延迟 1~5 秒再落子。
     */
    public void maybePlayBot(GameRoom room) {
        roomGameStateService.syncRoomFromDbIfBehind(room);
        if (applyOnlineClockTimeouts(room)) {
            broadcastState(room);
            return;
        }
        if (room.isGameOver()) {
            return;
        }
        if (room.isUndoPending()) {
            return;
        }
        if (room.isDrawPending()) {
            return;
        }
        int cur = room.getCurrent();
        if (cur == Stone.WHITE && !room.isWhiteIsBot()) {
            return;
        }
        if (cur == Stone.BLACK && !room.isBlackIsBot()) {
            return;
        }
        if (cur != Stone.WHITE && cur != Stone.BLACK) {
            return;
        }
        String roomId = room.getRoomId();
        cancelPendingBotMove(roomId);
        long delayMs = 1000L + ThreadLocalRandom.current().nextInt(4001);
        ScheduledFuture<?>[] holder = new ScheduledFuture<?>[1];
        holder[0] =
                botScheduler.schedule(
                        () -> {
                            try {
                                if (holder[0].isCancelled()) {
                                    return;
                                }
                                ScheduledFuture<?> current = pendingBotMoves.get(roomId);
                                if (current != holder[0]) {
                                    return;
                                }
                                playBotMoveIfStillValid(room);
                            } finally {
                                pendingBotMoves.remove(roomId, holder[0]);
                            }
                        },
                        delayMs,
                        TimeUnit.MILLISECONDS);
        pendingBotMoves.put(roomId, holder[0]);
    }

    /** 立即执行人机落子（调用前已确认轮到人机方等条件）。 */
    private void playBotMoveIfStillValid(GameRoom room) {
        roomGameStateService.syncRoomFromDbIfBehind(room);
        if (applyOnlineClockTimeouts(room)) {
            broadcastState(room);
            return;
        }
        if (room.isGameOver()) {
            return;
        }
        if (room.isUndoPending()) {
            return;
        }
        if (room.isDrawPending()) {
            return;
        }
        int botColor = room.getCurrent();
        if (botColor == Stone.WHITE && !room.isWhiteIsBot()) {
            return;
        }
        if (botColor == Stone.BLACK && !room.isBlackIsBot()) {
            return;
        }
        int[][] copy = room.getBoardCopy();
        int size = room.getSize();
        int[] mv =
                GomokuAiEngine.chooseMove(
                        copy,
                        size,
                        botColor,
                        GomokuAiEngine.nextBotSearchDepthInRange(
                                room.getBotSearchDepthMin(), room.getBotSearchDepthMax()),
                        BotAiStyle.fromOrdinal(room.getBotAiStyleOrdinal()));
        String err = room.tryMove(botColor, mv[0], mv[1]);
        if (err != null) {
            return;
        }
        if (!roomGameStateService.tryPersist(room)) {
            roomGameStateService.forceReloadFromDb(room);
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    /**
     * 人机对黑方悔棋：延迟 1~3 秒后，70% 同意、30% 拒绝。
     */
    private void scheduleBotUndoResponse(GameRoom room) {
        String roomId = room.getRoomId();
        cancelPendingBotUndoResponse(roomId);
        long delayMs = 1000L + ThreadLocalRandom.current().nextInt(2001);
        ScheduledFuture<?>[] holder = new ScheduledFuture<?>[1];
        holder[0] =
                botScheduler.schedule(
                        () -> {
                            try {
                                if (holder[0].isCancelled()) {
                                    return;
                                }
                                ScheduledFuture<?> current = pendingBotUndoResponses.get(roomId);
                                if (current != holder[0]) {
                                    return;
                                }
                                roomGameStateService.syncRoomFromDbIfBehind(room);
                                if (!room.isWhiteIsBot()
                                        || !room.isUndoPending()
                                        || room.getPendingUndoRequesterColor() == null
                                        || room.getPendingUndoRequesterColor() != Stone.BLACK) {
                                    return;
                                }
                                boolean reject = ThreadLocalRandom.current().nextDouble() < 0.3;
                                String err =
                                        reject
                                                ? room.rejectUndo(Stone.WHITE)
                                                : room.acceptUndo(Stone.WHITE);
                                if (err != null) {
                                    broadcastState(room);
                                    return;
                                }
                                if (!roomGameStateService.tryPersist(room)) {
                                    roomGameStateService.forceReloadFromDb(room);
                                    broadcastState(room);
                                    return;
                                }
                                broadcastState(room);
                                maybePlayBot(room);
                            } finally {
                                pendingBotUndoResponses.remove(roomId, holder[0]);
                            }
                        },
                        delayMs,
                        TimeUnit.MILLISECONDS);
        pendingBotUndoResponses.put(roomId, holder[0]);
    }

    /** 黑方人机对白方悔棋：延迟应答。 */
    private void scheduleBlackBotUndoResponse(GameRoom room) {
        String roomId = room.getRoomId();
        cancelPendingBotUndoResponse(roomId);
        long delayMs = 1000L + ThreadLocalRandom.current().nextInt(2001);
        ScheduledFuture<?>[] holder = new ScheduledFuture<?>[1];
        holder[0] =
                botScheduler.schedule(
                        () -> {
                            try {
                                if (holder[0].isCancelled()) {
                                    return;
                                }
                                ScheduledFuture<?> current = pendingBotUndoResponses.get(roomId);
                                if (current != holder[0]) {
                                    return;
                                }
                                roomGameStateService.syncRoomFromDbIfBehind(room);
                                if (!room.isBlackIsBot()
                                        || !room.isUndoPending()
                                        || room.getPendingUndoRequesterColor() == null
                                        || room.getPendingUndoRequesterColor() != Stone.WHITE) {
                                    return;
                                }
                                boolean reject = ThreadLocalRandom.current().nextDouble() < 0.3;
                                String err =
                                        reject
                                                ? room.rejectUndo(Stone.BLACK)
                                                : room.acceptUndo(Stone.BLACK);
                                if (err != null) {
                                    broadcastState(room);
                                    return;
                                }
                                if (!roomGameStateService.tryPersist(room)) {
                                    roomGameStateService.forceReloadFromDb(room);
                                    broadcastState(room);
                                    return;
                                }
                                broadcastState(room);
                                maybePlayBot(room);
                            } finally {
                                pendingBotUndoResponses.remove(roomId, holder[0]);
                            }
                        },
                        delayMs,
                        TimeUnit.MILLISECONDS);
        pendingBotUndoResponses.put(roomId, holder[0]);
    }

    /** 人机对黑方和棋：延迟 1~3 秒后，70% 同意、30% 拒绝。 */
    private void scheduleBotDrawResponse(GameRoom room) {
        String roomId = room.getRoomId();
        cancelPendingBotDrawResponse(roomId);
        long delayMs = 1000L + ThreadLocalRandom.current().nextInt(2001);
        ScheduledFuture<?>[] holder = new ScheduledFuture<?>[1];
        holder[0] =
                botScheduler.schedule(
                        () -> {
                            try {
                                if (holder[0].isCancelled()) {
                                    return;
                                }
                                ScheduledFuture<?> current = pendingBotDrawResponses.get(roomId);
                                if (current != holder[0]) {
                                    return;
                                }
                                roomGameStateService.syncRoomFromDbIfBehind(room);
                                if (!room.isWhiteIsBot()
                                        || !room.isDrawPending()
                                        || room.getPendingDrawRequesterColor() == null
                                        || room.getPendingDrawRequesterColor() != Stone.BLACK) {
                                    return;
                                }
                                boolean reject = ThreadLocalRandom.current().nextDouble() < 0.3;
                                String err =
                                        reject
                                                ? room.rejectDraw(Stone.WHITE)
                                                : room.acceptDraw(Stone.WHITE);
                                if (err != null) {
                                    broadcastState(room);
                                    return;
                                }
                                if (!roomGameStateService.tryPersist(room)) {
                                    roomGameStateService.forceReloadFromDb(room);
                                    broadcastState(room);
                                    return;
                                }
                                broadcastState(room);
                                maybePlayBot(room);
                            } finally {
                                pendingBotDrawResponses.remove(roomId, holder[0]);
                            }
                        },
                        delayMs,
                        TimeUnit.MILLISECONDS);
        pendingBotDrawResponses.put(roomId, holder[0]);
    }

    private void scheduleBlackBotDrawResponse(GameRoom room) {
        String roomId = room.getRoomId();
        cancelPendingBotDrawResponse(roomId);
        long delayMs = 1000L + ThreadLocalRandom.current().nextInt(2001);
        ScheduledFuture<?>[] holder = new ScheduledFuture<?>[1];
        holder[0] =
                botScheduler.schedule(
                        () -> {
                            try {
                                if (holder[0].isCancelled()) {
                                    return;
                                }
                                ScheduledFuture<?> current = pendingBotDrawResponses.get(roomId);
                                if (current != holder[0]) {
                                    return;
                                }
                                roomGameStateService.syncRoomFromDbIfBehind(room);
                                if (!room.isBlackIsBot()
                                        || !room.isDrawPending()
                                        || room.getPendingDrawRequesterColor() == null
                                        || room.getPendingDrawRequesterColor() != Stone.WHITE) {
                                    return;
                                }
                                boolean reject = ThreadLocalRandom.current().nextDouble() < 0.3;
                                String err =
                                        reject
                                                ? room.rejectDraw(Stone.BLACK)
                                                : room.acceptDraw(Stone.BLACK);
                                if (err != null) {
                                    broadcastState(room);
                                    return;
                                }
                                if (!roomGameStateService.tryPersist(room)) {
                                    roomGameStateService.forceReloadFromDb(room);
                                    broadcastState(room);
                                    return;
                                }
                                broadcastState(room);
                                maybePlayBot(room);
                            } finally {
                                pendingBotDrawResponses.remove(roomId, holder[0]);
                            }
                        },
                        delayMs,
                        TimeUnit.MILLISECONDS);
        pendingBotDrawResponses.put(roomId, holder[0]);
    }

    private void cancelPendingBotMove(String roomId) {
        ScheduledFuture<?> f = pendingBotMoves.remove(roomId);
        if (f != null) {
            f.cancel(false);
        }
    }

    private void cancelPendingBotUndoResponse(String roomId) {
        ScheduledFuture<?> f = pendingBotUndoResponses.remove(roomId);
        if (f != null) {
            f.cancel(false);
        }
    }

    private void cancelPendingBotDrawResponse(String roomId) {
        ScheduledFuture<?> f = pendingBotDrawResponses.remove(roomId);
        if (f != null) {
            f.cancel(false);
        }
    }

    private void cancelPendingBotTasks(String roomId) {
        cancelPendingBotMove(roomId);
        cancelPendingBotUndoResponse(roomId);
        cancelPendingBotDrawResponse(roomId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        if (Boolean.TRUE.equals(session.getAttributes().get(ATTR_SPECTATOR))) {
            GameRoom room = (GameRoom) session.getAttributes().get(ATTR_ROOM);
            if (room == null) {
                return;
            }
            synchronized (room) {
                if (room.getSpectatorSession() == session) {
                    room.setSpectatorSession(null);
                }
            }
            roomGameStateService.tryPersist(room);
            roomSessionTracker.unregister(room.getRoomId());
            broadcastState(room);
            return;
        }
        GameRoom room = (GameRoom) session.getAttributes().get(ATTR_ROOM);
        Integer color = (Integer) session.getAttributes().get(ATTR_COLOR);
        if (room == null || color == null) {
            return;
        }
        synchronized (room) {
            if (color == Stone.BLACK && room.getBlackSession() == session) {
                room.setBlackSession(null);
            }
            if (color == Stone.WHITE && room.getWhiteSession() == session) {
                room.setWhiteSession(null);
            }
        }
        if (color == Stone.BLACK) {
            room.setClusterBlackConnected(false);
        } else if (!room.isWhiteIsBot()) {
            room.setClusterWhiteConnected(false);
        }
        roomGameStateService.tryPersist(room);
        roomSessionTracker.unregister(room.getRoomId());
        broadcastState(room);
    }

    public void broadcastState(GameRoom room) {
        WebSocketSession bs = room.getBlackSession();
        WebSocketSession ws = room.getWhiteSession();
        WebSocketSession sp = room.getSpectatorSession();
        if (bs != null && bs.isOpen()) {
            sendToSession(bs, stateJson(room, Stone.BLACK, false));
        }
        if (ws != null && ws.isOpen()) {
            sendToSession(ws, stateJson(room, Stone.WHITE, false));
        }
        if (sp != null && sp.isOpen()) {
            sendToSession(sp, stateJson(room, Stone.BLACK, true));
        }
    }

    private void sendToSession(WebSocketSession session, TextMessage msg) {
        try {
            if (session.isOpen()) {
                session.sendMessage(msg);
            }
        } catch (Exception ignored) {
            // ignore
        }
    }

    /** 对局双方棋子皮肤：与 /api/me/rating 展示逻辑一致（装备槽优先）。 */
    private String pieceSkinIdForSeat(long userId) {
        return pieceSkinSelectionService.resolveEquippedPieceSkinForBroadcast(userId);
    }

    private TextMessage stateJson(GameRoom room, int yourColor, boolean spectator) {
        try {
            ObjectNode n = objectMapper.createObjectNode();
            n.put("type", "STATE");
            n.put("roomId", room.getRoomId());
            n.put("boardSize", room.getSize());
            n.set("board", objectMapper.valueToTree(room.getBoardCopy()));
            n.put("current", room.getCurrent());
            n.put("gameOver", room.isGameOver());
            if (room.getWinner() == null) {
                n.putNull("winner");
            } else {
                n.put("winner", room.getWinner());
            }
            n.put("matchRound", room.getMatchRound());
            n.put("spectator", spectator);
            if (spectator) {
                n.putNull("yourColor");
            } else {
                n.put("yourColor", yourColor);
            }
            n.put("blackPieceSkinId", pieceSkinIdForSeat(room.getBlackUserId()));
            Long whiteUid = room.getWhiteUserId();
            if (whiteUid == null) {
                n.put("whitePieceSkinId", PieceSkinSelectionService.SKIN_BASIC);
            } else {
                n.put("whitePieceSkinId", pieceSkinIdForSeat(whiteUid));
            }
            boolean blackHere =
                    room.isBlackIsBot()
                            || (room.getBlackSession() != null && room.getBlackSession().isOpen())
                            || room.isClusterBlackConnected();
            n.put("blackConnected", blackHere);
            boolean whiteHere =
                    room.isWhiteIsBot()
                            || (room.getWhiteSession() != null && room.getWhiteSession().isOpen())
                            || room.isClusterWhiteConnected();
            n.put("whiteConnected", whiteHere);
            n.put("whiteIsBot", room.isWhiteIsBot());
            n.put("blackIsBot", room.isBlackIsBot());
            Integer pr = room.getPendingRematchRequesterColor();
            n.put("rematchPending", pr != null);
            if (pr == null) {
                n.putNull("rematchRequesterColor");
            } else {
                n.put("rematchRequesterColor", pr);
            }
            n.put("undoPending", room.isUndoPending());
            if (room.getPendingUndoRequesterColor() == null) {
                n.putNull("undoRequesterColor");
            } else {
                n.put("undoRequesterColor", room.getPendingUndoRequesterColor());
            }
            n.put("drawPending", room.isDrawPending());
            if (room.getPendingDrawRequesterColor() == null) {
                n.putNull("drawRequesterColor");
            } else {
                n.put("drawRequesterColor", room.getPendingDrawRequesterColor());
            }
            n.put("clockMoveDeadlineWallMs", room.getClockMoveDeadlineWallMs());
            n.put("clockGameDeadlineWallMs", room.getClockGameDeadlineWallMs());
            n.put("clockPaused", room.isClockPaused());
            String ger = room.getGameEndReason();
            if (ger == null || ger.isEmpty()) {
                n.putNull("gameEndReason");
            } else {
                n.put("gameEndReason", ger);
            }
            return new TextMessage(objectMapper.writeValueAsString(n));
        } catch (Exception e) {
            return new TextMessage("{\"type\":\"ERROR\",\"message\":\"序列化失败\"}");
        }
    }

    private TextMessage error(String msg) {
        try {
            ObjectNode n = objectMapper.createObjectNode();
            n.put("type", "ERROR");
            n.put("message", msg);
            return new TextMessage(objectMapper.writeValueAsString(n));
        } catch (Exception e) {
            return new TextMessage("{\"type\":\"ERROR\",\"message\":\"错误\"}");
        }
    }

    private void sendError(WebSocketSession session, String msg) throws Exception {
        sendToSession(session, error(msg));
    }

    private static String first(List<String> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }
}
