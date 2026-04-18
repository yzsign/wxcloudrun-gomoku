-- =============================================================================
-- v32：创建杂货铺表并灌入与现网一致的种子数据（可重复执行）
-- =============================================================================
-- 与 PieceSkinRedeemService（皮肤/主题 200 分）、ConsumableService（短剑 2 分/个）对齐。
-- shop_items：INSERT IGNORE 按 item_code 去重。
-- shop_item_prices：NOT EXISTS 避免重复插入定价行。
-- =============================================================================

-- 商品主档：与客户端杂货铺列表一一对应
CREATE TABLE IF NOT EXISTS `shop_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_code` VARCHAR(64) NOT NULL COMMENT '稳定业务键',
  `shop_category` VARCHAR(32) NOT NULL COMMENT 'piece_skin | theme | consumable',
  `redeem_mode` VARCHAR(32) NOT NULL COMMENT 'FREE|CHECKIN_UNLOCK|POINTS_ONE_TIME|POINTS_PER_UNIT',
  `display_label` VARCHAR(128) NOT NULL COMMENT '展示名',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否上架',
  `consumable_kind` VARCHAR(32) DEFAULT NULL COMMENT '消耗品 kind',
  `client_row_id` VARCHAR(64) DEFAULT NULL COMMENT '前端卡片 id',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_items_code` (`item_code`),
  KEY `idx_shop_items_cat_sort` (`shop_category`, `sort_order`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='杂货铺商品主档';

-- 定价：仅 POINTS_* 商品有行；basic/tuan_moe 无积分价故不插 shop_item_prices
CREATE TABLE IF NOT EXISTS `shop_item_prices` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_item_id` BIGINT UNSIGNED NOT NULL COMMENT 'shop_items.id',
  `currency` VARCHAR(32) NOT NULL DEFAULT 'ACTIVITY_POINTS' COMMENT '团团积分',
  `amount` INT UNSIGNED NOT NULL COMMENT '价格',
  `unit_type` VARCHAR(32) NOT NULL DEFAULT 'ONE_TIME_UNLOCK' COMMENT 'ONE_TIME_UNLOCK|PER_UNIT',
  `valid_from` DATETIME(3) DEFAULT NULL COMMENT '生效起',
  `valid_to` DATETIME(3) DEFAULT NULL COMMENT '生效止',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_shop_prices_item` (`shop_item_id`, `currency`, `valid_from`, `valid_to`),
  CONSTRAINT `fk_shop_prices_item` FOREIGN KEY (`shop_item_id`) REFERENCES `shop_items` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='杂货铺定价';

-- 六条商品：基础皮、团团、青萄、青瓷、水墨、短剑（与 themes.getPieceSkinCatalog 一致）
INSERT IGNORE INTO `shop_items` (`item_code`, `shop_category`, `redeem_mode`, `display_label`, `sort_order`, `enabled`, `consumable_kind`, `client_row_id`) VALUES
('basic', 'piece_skin', 'FREE', '基础黑白', 10, 1, NULL, 'basic'),
('tuan_moe', 'piece_skin', 'CHECKIN_UNLOCK', '团团萌肤', 20, 1, NULL, 'tuan_moe'),
('qingtao_libai', 'piece_skin', 'POINTS_ONE_TIME', '青萄荔白', 30, 1, NULL, NULL),
('mint', 'theme', 'POINTS_ONE_TIME', '青瓷', 40, 1, NULL, NULL),
('ink', 'theme', 'POINTS_ONE_TIME', '水墨', 50, 1, NULL, NULL),
('dagger', 'consumable', 'POINTS_PER_UNIT', '短剑', 60, 1, 'dagger', 'dagger_skill');

-- 皮肤/主题：各 200 积分一次性解锁
INSERT INTO `shop_item_prices` (`shop_item_id`, `currency`, `amount`, `unit_type`)
SELECT s.id, 'ACTIVITY_POINTS', 200, 'ONE_TIME_UNLOCK'
FROM shop_items s
WHERE s.item_code IN ('qingtao_libai', 'mint', 'ink')
  AND NOT EXISTS (
    SELECT 1 FROM shop_item_prices p
    WHERE p.shop_item_id = s.id AND p.currency = 'ACTIVITY_POINTS'
      AND p.unit_type = 'ONE_TIME_UNLOCK' AND p.valid_to IS NULL
  );

-- 短剑：2 积分 / 个（与 user_consumables 扣减粒度一致）
INSERT INTO `shop_item_prices` (`shop_item_id`, `currency`, `amount`, `unit_type`)
SELECT s.id, 'ACTIVITY_POINTS', 2, 'PER_UNIT'
FROM shop_items s
WHERE s.item_code = 'dagger'
  AND NOT EXISTS (
    SELECT 1 FROM shop_item_prices p
    WHERE p.shop_item_id = s.id AND p.currency = 'ACTIVITY_POINTS'
      AND p.unit_type = 'PER_UNIT' AND p.valid_to IS NULL
  );
