-- =============================================================================
-- 杂货铺目录与定价（与小程序 themes.getPieceSkinCatalog、后端兑换逻辑对应）
-- =============================================================================
-- shop_items：上架商品元数据（是否积分兑换、展示名、排序、是否下架）。
-- shop_item_prices：积分价；无行表示「不卖积分 / 仅条件解锁」（见 redeem_mode）。
-- 用户侧拥有关系：user_piece_skin_unlocks、user_consumables、user_rating.activity_points。
-- =============================================================================

CREATE TABLE IF NOT EXISTS `shop_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_code` VARCHAR(64) NOT NULL COMMENT '稳定业务键：basic、tuan_moe、qingtao_libai、mint、ink、dagger',
  `shop_category` VARCHAR(32) NOT NULL COMMENT 'piece_skin=棋子皮肤 | theme=棋盘主题 | consumable=消耗品',
  `redeem_mode` VARCHAR(32) NOT NULL COMMENT 'FREE=默认已拥有 | CHECKIN_UNLOCK=签到等 | POINTS_ONE_TIME=积分买断皮肤/主题 | POINTS_PER_UNIT=积分按件买短剑',
  `display_label` VARCHAR(128) NOT NULL COMMENT '杂货铺卡片展示名',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '列表排序，越小越靠前',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=上架 0=下架（不下发/不可兑）',
  `consumable_kind` VARCHAR(32) DEFAULT NULL COMMENT '仅 consumable：与 POST consumables 的 kind 一致，如 dagger',
  `client_row_id` VARCHAR(64) DEFAULT NULL COMMENT '前端列表项 id，如 dagger_skill；普通皮肤可与 item_code 同',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_items_code` (`item_code`),
  KEY `idx_shop_items_cat_sort` (`shop_category`, `sort_order`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='杂货铺商品主档';

CREATE TABLE IF NOT EXISTS `shop_item_prices` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_item_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 shop_items.id',
  `currency` VARCHAR(32) NOT NULL DEFAULT 'ACTIVITY_POINTS' COMMENT '货币：现阶段为 ACTIVITY_POINTS（团团积分）',
  `amount` INT UNSIGNED NOT NULL COMMENT '价格数值：ONE_TIME_UNLOCK=一次性解锁总价；PER_UNIT=每个消耗品所需积分',
  `unit_type` VARCHAR(32) NOT NULL DEFAULT 'ONE_TIME_UNLOCK' COMMENT 'ONE_TIME_UNLOCK=皮肤/主题 | PER_UNIT=短剑等堆叠消耗品',
  `valid_from` DATETIME(3) DEFAULT NULL COMMENT '生效起始；NULL=不限制起点',
  `valid_to` DATETIME(3) DEFAULT NULL COMMENT '生效结束；NULL=长期有效；用于促销或调价历史',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_shop_prices_item` (`shop_item_id`, `currency`, `valid_from`, `valid_to`),
  CONSTRAINT `fk_shop_prices_item` FOREIGN KEY (`shop_item_id`) REFERENCES `shop_items` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='杂货铺定价（同一商品可多行表示不同时间段价格）';
