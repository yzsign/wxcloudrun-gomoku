-- =============================================================================
-- 用户消耗品库存（独立于 users；与杂货铺 consumable 商品、兑换/use API 对应）
-- =============================================================================
-- 与 shop_items 中 item_code=dagger、consumable_kind=dagger 对应；数量存在本表。

CREATE TABLE IF NOT EXISTS `user_consumables` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 id，外键 users.id',
  `kind` VARCHAR(32) NOT NULL COMMENT '种类，如 dagger=短剑',
  `quantity` INT NOT NULL DEFAULT 0 COMMENT '持有数量；兑换 +1，对局内使用 -1',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_consumable` (`user_id`, `kind`),
  KEY `idx_user_consumables_user` (`user_id`),
  CONSTRAINT `fk_user_consumables_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户消耗品库存';
