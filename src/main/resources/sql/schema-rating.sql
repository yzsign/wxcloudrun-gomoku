-- 与 rule.md v1.1 对齐：天梯分、对局归档、积分流水、房间参与者
-- 新库：可与 schema-user.sql 一并执行。已有库请用 migration-v2-rating.sql

CREATE TABLE IF NOT EXISTS `room_participants` (
  `room_id` VARCHAR(32) NOT NULL,
  `black_user_id` BIGINT UNSIGNED NOT NULL,
  `white_user_id` BIGINT UNSIGNED DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`room_id`),
  KEY `idx_black` (`black_user_id`),
  KEY `idx_white` (`white_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `games` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `room_id` VARCHAR(32) NOT NULL,
  `black_user_id` BIGINT UNSIGNED NOT NULL,
  `white_user_id` BIGINT UNSIGNED NOT NULL,
  `total_steps` INT UNSIGNED NOT NULL,
  `outcome` VARCHAR(16) NOT NULL COMMENT 'BLACK_WIN, WHITE_WIN, DRAW',
  `runaway_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '逃跑/超时判负方',
  `black_elo_before` INT NOT NULL,
  `white_elo_before` INT NOT NULL,
  `black_elo_after` INT NOT NULL,
  `white_elo_after` INT NOT NULL,
  `black_elo_delta` INT NOT NULL,
  `white_elo_delta` INT NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_id` (`room_id`),
  KEY `idx_black_user` (`black_user_id`, `created_at`),
  KEY `idx_white_user` (`white_user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `rating_change_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `room_id` VARCHAR(32) NOT NULL,
  `opponent_user_id` BIGINT UNSIGNED NOT NULL,
  `elo_before` INT NOT NULL,
  `elo_after` INT NOT NULL,
  `delta` INT NOT NULL,
  `total_steps` INT UNSIGNED NOT NULL,
  `runaway` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
