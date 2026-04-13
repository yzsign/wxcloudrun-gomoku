package com.gomoku.sync.websocket;

import com.gomoku.sync.service.SessionJwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 用户级 WebSocket：/ws/user?sessionToken=...（与房间 /ws/gomoku 分离，见 friend-request-social-spec §5）
 */
@Component
public class UserWebSocketHandler extends TextWebSocketHandler {

    static final String ATTR_USER_ID = "userId";

    private static final Logger log = LoggerFactory.getLogger(UserWebSocketHandler.class);

    private final SessionJwtService sessionJwtService;
    private final UserWebSocketRegistry registry;

    public UserWebSocketHandler(SessionJwtService sessionJwtService, UserWebSocketRegistry registry) {
        this.sessionJwtService = sessionJwtService;
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(Objects.requireNonNull(CloseStatus.BAD_DATA));
            return;
        }
        Map<String, List<String>> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        String sessionToken = first(params.get("sessionToken"));
        if (sessionToken == null || sessionToken.isEmpty()) {
            session.close(Objects.requireNonNull(CloseStatus.BAD_DATA));
            return;
        }
        Optional<Long> userId = sessionJwtService.parseUserId(sessionToken);
        if (!userId.isPresent()) {
            session.close(Objects.requireNonNull(CloseStatus.NOT_ACCEPTABLE));
            return;
        }
        session.getAttributes().put(ATTR_USER_ID, userId.get());
        registry.register(userId.get(), session);
        log.debug("user ws connected userId={}", userId.get());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        String p = message.getPayload();
        if ("ping".equalsIgnoreCase(p) || "{\"type\":\"PING\"}".equalsIgnoreCase(p.trim())) {
            session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
            return;
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        registry.unregister(session);
    }

    private static String first(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
}
