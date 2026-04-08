-- 杂货铺「装备槽」：每用户每个 category 至多一件（便于扩展新种类：音效、头像框等）
CREATE TABLE IF NOT EXISTS `user_equipped_cosmetics` (
  `user_id` BIGINT UNSIGNED NOT NULL,
  `category` VARCHAR(32) NOT NULL COMMENT 'PIECE_SKIN=棋子皮肤 THEME=界面棋盘主题 等，与业务常量一致',
  `item_id` VARCHAR(64) NOT NULL COMMENT '该种类下的装备 id，与客户端一致',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`, `category`),
  KEY `idx_category` (`category`),
  CONSTRAINT `fk_user_equipped_cosmetics_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
