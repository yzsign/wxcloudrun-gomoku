-- 好友 PvP 观战票：多实例下须持久化，否则 HTTP 发 token 的实例与 WS 所在实例内存不一致会导致鉴权失败
ALTER TABLE room_participants
    ADD COLUMN friend_watch_token VARCHAR(64) NULL DEFAULT NULL
        COMMENT '非残局 PVP 好友观战 WebSocket 用 token' AFTER white_token;
