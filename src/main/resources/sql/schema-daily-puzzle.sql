-- 新库可与其它 schema-*.sql 一并执行；已有库请用 migration-v23-daily-puzzle.sql

CREATE TABLE IF NOT EXISTS `daily_puzzle` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '题目标题',
  `difficulty` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '难度 1-5',
  `board_size` TINYINT UNSIGNED NOT NULL DEFAULT 15,
  `board_json` LONGTEXT NOT NULL COMMENT '初始棋盘 JSON：二维 int 数组，与 Stone 一致',
  `side_to_move` TINYINT NOT NULL COMMENT '下一手行棋方：1=黑 2=白',
  `goal` VARCHAR(32) NOT NULL DEFAULT 'WIN' COMMENT 'WIN=先手方五连胜；DRAW=终局满盘无胜方',
  `max_user_moves` INT UNSIGNED NULL COMMENT '可选：限制提交手顺总步数上限',
  `solution_moves_json` LONGTEXT NULL COMMENT '可选：参考答案手顺 JSON',
  `hint_text` VARCHAR(512) NULL DEFAULT NULL COMMENT '可选：文字提示',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=上架 0=下架',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `daily_puzzle_schedule` (
  `puzzle_date` DATE NOT NULL COMMENT '服务器日历日（与签到一致 Asia/Shanghai）',
  `puzzle_id` BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`puzzle_date`),
  KEY `idx_puzzle` (`puzzle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_daily_puzzle` (
  `user_id` BIGINT UNSIGNED NOT NULL,
  `puzzle_date` DATE NOT NULL,
  `puzzle_id` BIGINT UNSIGNED NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS, SOLVED',
  `attempt_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `best_moves_to_win` INT UNSIGNED NULL COMMENT '取胜时总步数（可选统计）',
  `last_attempt_moves_json` LONGTEXT NULL,
  `solved_at` DATETIME(3) NULL DEFAULT NULL,
  `hint_used` TINYINT(1) NOT NULL DEFAULT 0,
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`, `puzzle_date`),
  KEY `idx_date` (`puzzle_date`, `status`),
  KEY `idx_puzzle` (`puzzle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
