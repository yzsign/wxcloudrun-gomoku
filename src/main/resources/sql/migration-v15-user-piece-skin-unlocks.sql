-- v15：用户棋子皮肤解锁表（多皮肤、积分兑换等可扩展；定价在业务层）
-- 可重复执行。

CREATE TABLE IF NOT EXISTS `user_piece_skin_unlocks` (
  `user_id` BIGINT UNSIGNED NOT NULL,
  `skin_id` VARCHAR(32) NOT NULL COMMENT '皮肤标识，与客户端一致，如 qingtao_libai',
  `unlock_source` VARCHAR(32) NOT NULL DEFAULT 'ACTIVITY_POINTS' COMMENT 'ACTIVITY_POINTS / ADMIN / MIGRATION 等',
  `points_spent` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本次解锁消耗的活跃积分；非积分途径为 0',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`, `skin_id`),
  KEY `idx_skin_id` (`skin_id`),
  CONSTRAINT `fk_user_piece_skin_unlocks_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
