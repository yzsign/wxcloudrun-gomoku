-- 签到汇总：每用户一行（与 users 分离；活动积分在 user_rating.activity_points）
CREATE TABLE IF NOT EXISTS `user_checkin_state` (
  `user_id` BIGINT UNSIGNED NOT NULL,
  `last_checkin_ymd` VARCHAR(10) DEFAULT NULL COMMENT '上次签到 YYYY-MM-DD（Asia/Shanghai）',
  `streak` INT NOT NULL DEFAULT 0 COMMENT '当前连续签到天数',
  `history_json` TEXT DEFAULT NULL COMMENT '已签到日期 JSON 数组，最多约500条',
  `piece_skin_tuan_moe_unlocked` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '连签7天解锁团团萌肤，永久有效',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_user_checkin_state_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
