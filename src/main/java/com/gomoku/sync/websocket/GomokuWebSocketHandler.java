package com.gomoku.sync.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gomoku.sync.ai.GomokuAiEngine;
import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.domain.Stone;
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

@Component
public class GomokuWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTR_ROOM = "room";
    private static final String ATTR_COLOR = "color";

    private final RoomService roomService;
    private final ObjectMapper objectMapper;
    private final SessionJwtService sessionJwtService;

    public GomokuWebSocketHandler(
            RoomService roomService,
            ObjectMapper objectMapper,
            SessionJwtService sessionJwtService) {
        this.roomService = roomService;
        this.objectMapper = objectMapper;
        this.sessionJwtService = sessionJwtService;
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

        broadcastState(room);
        maybePlayBot(room);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        GameRoom room = (GameRoom) session.getAttributes().get(ATTR_ROOM);
        Integer color = (Integer) session.getAttributes().get(ATTR_COLOR);
        if (room == null || color == null) {
            return;
        }

        JsonNode root = objectMapper.readTree(message.getPayload());
        String type = root.path("type").asText("");
        if ("MOVE".equals(type)) {
            handleMove(session, room, color, root);
        } else if ("RESET".equals(type)) {
            handleReset(session, room);
        } else if ("UNDO_REQUEST".equals(type)) {
            handleUndoRequest(session, room, color);
        } else if ("UNDO_ACCEPT".equals(type)) {
            handleUndoAccept(session, room, color);
        } else if ("UNDO_REJECT".equals(type)) {
            handleUndoReject(session, room, color);
        } else if ("UNDO_CANCEL".equals(type)) {
            handleUndoCancel(session, room, color);
        } else {
            sendToSession(session, error("未知消息类型: " + type));
        }
    }

    private void handleMove(WebSocketSession session, GameRoom room, int color, JsonNode root) {
        int r = root.path("r").asInt(-1);
        int c = root.path("c").asInt(-1);
        String err = room.tryMove(color, r, c);
        if (err != null) {
            sendToSession(session, error(err));
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    private void handleUndoRequest(WebSocketSession session, GameRoom room, int color) {
        String err = room.requestUndo(color);
        if (err != null) {
            sendToSession(session, error(err));
            return;
        }
        if (room.isWhiteIsBot()
                && room.isUndoPending()
                && room.getPendingUndoRequesterColor() != null
                && room.getPendingUndoRequesterColor() == Stone.BLACK) {
            String err2 = room.acceptUndo(Stone.WHITE);
            if (err2 != null) {
                broadcastState(room);
                return;
            }
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    private void handleUndoAccept(WebSocketSession session, GameRoom room, int color) {
        String err = room.acceptUndo(color);
        if (err != null) {
            sendToSession(session, error(err));
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    private void handleUndoReject(WebSocketSession session, GameRoom room, int color) {
        String err = room.rejectUndo(color);
        if (err != null) {
            sendToSession(session, error(err));
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    private void handleUndoCancel(WebSocketSession session, GameRoom room, int color) {
        String err = room.cancelUndoRequest(color);
        if (err != null) {
            sendToSession(session, error(err));
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    /** 对局结束后任一方可申请重新开始（同房间） */
    private void handleReset(WebSocketSession session, GameRoom room) {
        room.getLock().lock();
        try {
            if (!room.isGameOver()) {
                // 对方已 reset、本端结算未关时：房间已是新局，重复 RESET 视为同步状态
                if (room.isBoardEmpty()) {
                    broadcastState(room);
                    return;
                }
                sendToSession(session, error("对局未结束，不能重置"));
                return;
            }
            room.resetMatch();
        } finally {
            room.getLock().unlock();
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    /**
     * 白方为数据库人机时：轮到白且非悔棋待定时代为落子。
     */
    private void maybePlayBot(GameRoom room) {
        if (!room.isWhiteIsBot() || room.isGameOver()) {
            return;
        }
        if (room.isUndoPending()) {
            return;
        }
        if (room.getCurrent() != Stone.WHITE) {
            return;
        }
        int[][] copy = room.getBoardCopy();
        int size = room.getSize();
        int[] mv = GomokuAiEngine.chooseMove(copy, size, Stone.WHITE);
        String err = room.tryMove(Stone.WHITE, mv[0], mv[1]);
        if (err != null) {
            return;
        }
        broadcastState(room);
        maybePlayBot(room);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
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
        broadcastState(room);
    }

    private void broadcastState(GameRoom room) {
        WebSocketSession bs = room.getBlackSession();
        WebSocketSession ws = room.getWhiteSession();
        if (bs != null && bs.isOpen()) {
            sendToSession(bs, stateJson(room, Stone.BLACK));
        }
        if (ws != null && ws.isOpen()) {
            sendToSession(ws, stateJson(room, Stone.WHITE));
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

    private TextMessage stateJson(GameRoom room, int yourColor) {
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
            n.put("yourColor", yourColor);
            n.put("blackConnected", room.getBlackSession() != null && room.getBlackSession().isOpen());
            boolean whiteHere =
                    room.isWhiteIsBot()
                            || (room.getWhiteSession() != null && room.getWhiteSession().isOpen());
            n.put("whiteConnected", whiteHere);
            n.put("whiteIsBot", room.isWhiteIsBot());
            n.put("undoPending", room.isUndoPending());
            if (room.getPendingUndoRequesterColor() == null) {
                n.putNull("undoRequesterColor");
            } else {
                n.put("undoRequesterColor", room.getPendingUndoRequesterColor());
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
