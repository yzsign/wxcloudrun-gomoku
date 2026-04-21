-- 对局复盘：每用户仅保留一条最新存档（与每日残局 daily_puzzle 表独立）
CREATE TABLE IF NOT EXISTS user_replay_study (
    user_id BIGINT NOT NULL PRIMARY KEY COMMENT '用户 id',
    moves_json LONGTEXT NOT NULL COMMENT '完整棋谱 [{r,c,color},...]',
    replay_step INT NOT NULL COMMENT '存档时回放到第几步（0=空盘）',
    board_json LONGTEXT NOT NULL COMMENT '第 replay_step 手后的盘面 int[][]',
    side_to_move TINYINT NOT NULL COMMENT '下一手 1黑2白',
    source_game_id BIGINT NULL COMMENT '可选：来源 games.id',
    black_piece_skin_id VARCHAR(64) NULL,
    white_piece_skin_id VARCHAR(64) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户对局复盘最新存档';
