package com.gomoku.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gomoku.sync.websocket.UserWebSocketRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 向本机已连接的用户 WS 推送 JSON；多实例部署时需配合 Redis/MQ 广播（当前未接）。
 */
@Service
public class UserWebSocketPushService {

    private static final Logger log = LoggerFactory.getLogger(UserWebSocketPushService.class);

    private final UserWebSocketRegistry registry;
    private final ObjectMapper objectMapper;

    public UserWebSocketPushService(UserWebSocketRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    public void sendToUser(long userId, ObjectNode envelope) {
        try {
            String json = objectMapper.writeValueAsString(envelope);
            WebSocketSession s = registry.getSession(userId).orElse(null);
            if (s != null && s.isOpen()) {
                synchronized (s) {
                    s.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.warn("user ws push failed userId={}: {}", userId, e.getMessage());
        }
    }

    public void friendRequestIncoming(
            long toUserId,
            long friendRequestId,
            long fromUserId,
            String fromNickname) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("friendRequestId", friendRequestId);
        payload.put("fromUserId", fromUserId);
        payload.put("fromNickname", fromNickname != null ? fromNickname : "");
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "FRIEND_REQUEST_INCOMING");
        root.set("payload", payload);
        sendToUser(toUserId, root);
    }

    public void friendRequestResolved(
            long userId,
            long friendRequestId,
            String outcome,
            long counterpartUserId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("friendRequestId", friendRequestId);
        payload.put("outcome", outcome);
        payload.put("counterpartUserId", counterpartUserId);
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "FRIEND_REQUEST_RESOLVED");
        root.set("payload", payload);
        sendToUser(userId, root);
    }

    public void friendshipUpdated(long userId, long peerUserId, boolean nowFriends) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("peerUserId", peerUserId);
        payload.put("nowFriends", nowFriends);
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "FRIENDSHIP_UPDATED");
        root.set("payload", payload);
        sendToUser(userId, root);
    }
}
