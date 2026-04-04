-- 多实例对局一致：棋盘与悔棋等状态持久化，state_version 乐观锁

CREATE TABLE IF NOT EXISTS `room_game_state` (
  `room_id` VARCHAR(32) NOT NULL,
  `state_json` LONGTEXT NOT NULL COMMENT 'GameRoomStateSnapshot JSON',
  `state_version` BIGINT NOT NULL DEFAULT 0,
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
