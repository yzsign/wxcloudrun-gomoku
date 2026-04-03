-- 已有 gomoku 库时执行：扩展 users 并创建 rating 相关表

ALTER TABLE `users`
  ADD COLUMN `elo_score` INT NOT NULL DEFAULT 1200 COMMENT '天梯分' AFTER `avatar_url`,
  ADD COLUMN `activity_points` INT NOT NULL DEFAULT 0 AFTER `elo_score`,
  ADD COLUMN `consecutive_wins` INT NOT NULL DEFAULT 0 AFTER `activity_points`,
  ADD COLUMN `consecutive_losses` INT NOT NULL DEFAULT 0 AFTER `consecutive_wins`,
  ADD COLUMN `today_net_change` INT NOT NULL DEFAULT 0 AFTER `consecutive_losses`,
  ADD COLUMN `elo_carry_over` INT NOT NULL DEFAULT 0 COMMENT '§7.1 单日上限溢出，次日释放' AFTER `today_net_change`,
  ADD COLUMN `last_rating_reset_date` DATE DEFAULT NULL COMMENT '日切与单日净变动统计日' AFTER `elo_carry_over`,
  ADD COLUMN `runaway_count` INT NOT NULL DEFAULT 0 AFTER `last_rating_reset_date`,
  ADD COLUMN `total_games` INT NOT NULL DEFAULT 0 AFTER `runaway_count`,
  ADD COLUMN `win_count` INT NOT NULL DEFAULT 0 AFTER `total_games`,
  ADD COLUMN `draw_count` INT NOT NULL DEFAULT 0 AFTER `win_count`,
  ADD COLUMN `season_end_score` INT DEFAULT NULL AFTER `draw_count`,
  ADD COLUMN `placement_fair_games` INT NOT NULL DEFAULT 0 COMMENT '无逃跑完成局数，用于定级' AFTER `season_end_score`,
  ADD COLUMN `newbie_match_games` INT NOT NULL DEFAULT 0 COMMENT '§6.3 已结算局数计数' AFTER `placement_fair_games`,
  ADD COLUMN `newbie_runaway_tally` INT NOT NULL DEFAULT 0 COMMENT '保护期内作为逃跑方次数' AFTER `newbie_match_games`,
  ADD COLUMN `low_trust` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '§7.4 低信誉' AFTER `newbie_runaway_tally`;

-- 若列已存在会报错，请按需注释掉已执行过的 ADD

-- 以下为新建表（与 schema-rating.sql 一致）

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
  `outcome` VARCHAR(16) NOT NULL,
  `runaway_user_id` BIGINT UNSIGNED DEFAULT NULL,
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
