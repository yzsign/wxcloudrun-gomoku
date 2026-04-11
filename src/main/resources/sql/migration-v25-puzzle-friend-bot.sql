-- 残局好友房：持久化初始盘面与行棋方（好友进房时重置）；黑方人机标记
ALTER TABLE room_participants
    ADD COLUMN puzzle_init_board_json TEXT NULL COMMENT '残局好友房：创建时的盘面 JSON，好友进房时重置' AFTER puzzle_room,
    ADD COLUMN puzzle_side_to_move TINYINT NULL COMMENT '1黑2白：好友进房后重置为该下一手' AFTER puzzle_init_board_json,
    ADD COLUMN black_is_bot TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=黑方为人机（好友执白时黑为 AI）' AFTER white_is_bot;
