-- 残局好友房：房主旁观 token；puzzle_room 标记（多实例加载 GameRoom 元数据）
ALTER TABLE room_participants
    ADD COLUMN observer_user_id BIGINT NULL COMMENT '房主旁观：与 black 同一用户时可仅连旁观 WS' AFTER black_token,
    ADD COLUMN observer_token VARCHAR(64) NULL COMMENT '房主旁观 WebSocket token' AFTER observer_user_id,
    ADD COLUMN puzzle_room TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=残局初始盘面房间' AFTER observer_token;
