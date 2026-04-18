-- =============================================================================
-- v33：爱心消耗品 kind=love、杂货铺上架、BOARD_SKILL_LOVE 装备槽（可重复执行）
-- =============================================================================

INSERT IGNORE INTO `shop_items` (`item_code`, `shop_category`, `redeem_mode`, `display_label`, `sort_order`, `enabled`, `consumable_kind`, `client_row_id`) VALUES
('love', 'consumable', 'POINTS_PER_UNIT', '爱心', 65, 1, 'love', 'love_skill');

INSERT INTO `shop_item_prices` (`shop_item_id`, `currency`, `amount`, `unit_type`)
SELECT s.id, 'ACTIVITY_POINTS', 2, 'PER_UNIT'
FROM shop_items s
WHERE s.item_code = 'love'
  AND NOT EXISTS (
    SELECT 1 FROM shop_item_prices p
    WHERE p.shop_item_id = s.id AND p.currency = 'ACTIVITY_POINTS'
      AND p.unit_type = 'PER_UNIT' AND p.valid_to IS NULL
  );
