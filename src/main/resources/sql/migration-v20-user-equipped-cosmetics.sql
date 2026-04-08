-- v20：装备槽表（棋子皮肤 / 界面主题各一类，每类一件；可扩展 category）
CREATE TABLE IF NOT EXISTS `user_equipped_cosmetics` (
  `user_id` BIGINT UNSIGNED NOT NULL,
  `category` VARCHAR(32) NOT NULL COMMENT 'PIECE_SKIN=棋子皮肤 THEME=界面棋盘主题 等',
  `item_id` VARCHAR(64) NOT NULL COMMENT '装备 id，与客户端一致',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`, `category`),
  KEY `idx_category` (`category`),
  CONSTRAINT `fk_user_equipped_cosmetics_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 从 users.piece_skin_id 迁入 PIECE_SKIN（与现有数据兼容）
INSERT INTO `user_equipped_cosmetics` (`user_id`, `category`, `item_id`)
SELECT `id`, 'PIECE_SKIN', `piece_skin_id`
FROM `users`
WHERE `piece_skin_id` IS NOT NULL AND CHAR_LENGTH(TRIM(`piece_skin_id`)) > 0
ON DUPLICATE KEY UPDATE `item_id` = VALUES(`item_id`);
