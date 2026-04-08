-- v12：签到数据独立表 user_checkin_state（users 不再存签到列）
-- 新库：与 schema-user.sql（无签到列）+ 本文件一并执行即可。
-- 若线上曾执行 migration-v11（users 上已有签到列），请先执行下方「可选迁移」再执行 migration-v13 删列。

CREATE TABLE IF NOT EXISTS `user_checkin_state` (
  `user_id` BIGINT UNSIGNED NOT NULL,
  `last_checkin_ymd` VARCHAR(10) DEFAULT NULL COMMENT '上次签到 YYYY-MM-DD（Asia/Shanghai）',
  `streak` INT NOT NULL DEFAULT 0 COMMENT '当前连续签到天数',
  `history_json` TEXT DEFAULT NULL COMMENT '已签到日期 JSON 数组',
  `piece_skin_tuan_moe_unlocked` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '连签7天解锁团团萌肤',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_user_checkin_state_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 可选：从 v11 的 users 列迁移（仅当存在 checkin_last_ymd 列时执行）==========
-- INSERT INTO user_checkin_state (user_id, last_checkin_ymd, streak, history_json, piece_skin_tuan_moe_unlocked)
-- SELECT id, checkin_last_ymd, checkin_streak, checkin_history_json, piece_skin_tuan_moe_unlocked
-- FROM users
-- ON DUPLICATE KEY UPDATE
--   last_checkin_ymd = VALUES(last_checkin_ymd),
--   streak = VALUES(streak),
--   history_json = VALUES(history_json),
--   piece_skin_tuan_moe_unlocked = VALUES(piece_skin_tuan_moe_unlocked);
-- 然后执行 migration-v13-users-drop-checkin-columns.sql
