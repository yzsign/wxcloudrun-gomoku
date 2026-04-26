-- v38：将天梯与统计从 users 拆至 user_rating（1:1，user_id = users.id）
-- 要求：MySQL 8.0.29+（DROP COLUMN IF EXISTS / DROP INDEX IF EXISTS）
-- 可重复执行：已拆分的库会跳过插入并无害化 DROP

CREATE TABLE IF NOT EXISTS `user_rating` (
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT 'users.id，1:1',
  `elo_score` INT NOT NULL DEFAULT 1200 COMMENT '天梯分',
  `title_name` VARCHAR(32) NULL DEFAULT NULL COMMENT '称号，与 rule.md §5 一致，随对局结算更新',
  `activity_points` INT NOT NULL DEFAULT 0 COMMENT '活跃积分（团团积分），与天梯分独立，见 rule.md §12',
  `consecutive_wins` INT NOT NULL DEFAULT 0 COMMENT '连胜场数；负局或和棋后归零',
  `consecutive_losses` INT NOT NULL DEFAULT 0 COMMENT '连败场数',
  `today_net_change` INT NOT NULL DEFAULT 0 COMMENT '当日天梯净变动累计，用于单日变动上限（与 elo_carry_over 配合）',
  `elo_carry_over` INT NOT NULL DEFAULT 0 COMMENT '单日净变动溢出，次日并入 elo_score',
  `last_rating_reset_date` DATE DEFAULT NULL COMMENT '日切日期：单日净变动统计与重置所依据的「当前日」',
  `runaway_count` INT NOT NULL DEFAULT 0 COMMENT '累计逃跑次数（作逃跑方）',
  `total_games` INT NOT NULL DEFAULT 0 COMMENT '已结算对局总局数',
  `win_count` INT NOT NULL DEFAULT 0 COMMENT '胜局数',
  `draw_count` INT NOT NULL DEFAULT 0 COMMENT '和棋局数',
  `season_end_score` INT DEFAULT NULL COMMENT '赛季末备份的天梯分（elo_score 快照）',
  `placement_fair_games` INT NOT NULL DEFAULT 0 COMMENT '无逃跑完成局数（定级）',
  `newbie_match_games` INT NOT NULL DEFAULT 0 COMMENT '§6.3 已结算局数',
  `newbie_runaway_tally` INT NOT NULL DEFAULT 0 COMMENT '保护期内作逃跑方次数',
  `low_trust` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=低信誉（rule.md §7.4）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '本行统计最后更新时间',
  PRIMARY KEY (`user_id`),
  KEY `idx_elo_score` (`elo_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 从旧 users 迁入（仅当尚未拆列时执行；已拆分的库自动跳过）
SET @users_has_elo := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND COLUMN_NAME = 'elo_score'
);
SET @copy_rating := IF(
  @users_has_elo > 0,
  'INSERT IGNORE INTO `user_rating` (`user_id`,`elo_score`,`title_name`,`activity_points`,`consecutive_wins`,`consecutive_losses`,`today_net_change`,`elo_carry_over`,`last_rating_reset_date`,`runaway_count`,`total_games`,`win_count`,`draw_count`,`season_end_score`,`placement_fair_games`,`newbie_match_games`,`newbie_runaway_tally`,`low_trust`) SELECT `id`,`elo_score`,`title_name`,`activity_points`,`consecutive_wins`,`consecutive_losses`,`today_net_change`,`elo_carry_over`,`last_rating_reset_date`,`runaway_count`,`total_games`,`win_count`,`draw_count`,`season_end_score`,`placement_fair_games`,`newbie_match_games`,`newbie_runaway_tally`,`low_trust` FROM `users`',
  'SELECT ''skip: users.elo_score absent, copy already done or new schema'' AS migration_v38_copy'
);
PREPARE migration_v38_copy FROM @copy_rating;
EXECUTE migration_v38_copy;
DEALLOCATE PREPARE migration_v38_copy;

-- 外键（可重复执行）
SET @fk_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_rating'
    AND CONSTRAINT_NAME = 'fk_user_rating_user'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @add_fk := IF(
  @fk_exists = 0,
  'ALTER TABLE `user_rating` ADD CONSTRAINT `fk_user_rating_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE',
  'SELECT ''skip: fk_user_rating_user already exists'' AS migration_v38_fk'
);
PREPARE migration_v38_fk FROM @add_fk;
EXECUTE migration_v38_fk;
DEALLOCATE PREPARE migration_v38_fk;

DROP INDEX IF EXISTS `idx_elo_score` ON `users`;

ALTER TABLE `users`
  DROP COLUMN IF EXISTS `elo_score`,
  DROP COLUMN IF EXISTS `title_name`,
  DROP COLUMN IF EXISTS `activity_points`,
  DROP COLUMN IF EXISTS `consecutive_wins`,
  DROP COLUMN IF EXISTS `consecutive_losses`,
  DROP COLUMN IF EXISTS `today_net_change`,
  DROP COLUMN IF EXISTS `elo_carry_over`,
  DROP COLUMN IF EXISTS `last_rating_reset_date`,
  DROP COLUMN IF EXISTS `runaway_count`,
  DROP COLUMN IF EXISTS `total_games`,
  DROP COLUMN IF EXISTS `win_count`,
  DROP COLUMN IF EXISTS `draw_count`,
  DROP COLUMN IF EXISTS `season_end_score`,
  DROP COLUMN IF EXISTS `placement_fair_games`,
  DROP COLUMN IF EXISTS `newbie_match_games`,
  DROP COLUMN IF EXISTS `newbie_runaway_tally`,
  DROP COLUMN IF EXISTS `low_trust`;
