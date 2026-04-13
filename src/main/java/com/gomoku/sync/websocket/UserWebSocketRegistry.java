package com.gomoku.sync.websocket;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户级 WS 连接：同一 userId 仅保留最新一条连接（重连覆盖）。
 */
@Component
public class UserWebSocketRegistry {

    private final ConcurrentHashMap<Long, WebSocketSession> byUserId = new ConcurrentHashMap<>();

    public void register(long userId, @NonNull WebSocketSession session) {
        WebSocketSession prev = byUserId.put(userId, session);
        if (prev != null && prev.isOpen() && !prev.getId().equals(session.getId())) {
            try {
                prev.close();
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    public void unregister(@NonNull WebSocketSession session) {
        Long uid = (Long) session.getAttributes().get(UserWebSocketHandler.ATTR_USER_ID);
        if (uid != null) {
            byUserId.remove(uid, session);
        }
    }

    public Optional<WebSocketSession> getSession(long userId) {
        return Optional.ofNullable(byUserId.get(userId));
    }
}
