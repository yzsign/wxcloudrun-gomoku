-- =============================================================================
-- v31：短剑等消耗品库存表 user_consumables；弃用 users.consumable_dagger_count（可重复执行）
-- =============================================================================
-- 步骤：1）建表 2）若旧库仍有 users.consumable_dagger_count 则迁入本表 3）删除 users 上该列
-- 迁移后服务端以 user_consumables 为准；与 shop_items（dagger）+ 兑换接口配合使用。
-- =============================================================================

-- 每用户每 kind 一行；数量与杂货铺兑换、对局内 use 扣减一致
CREATE TABLE IF NOT EXISTS `user_consumables` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 id，关联 users.id',
  `kind` VARCHAR(32) NOT NULL COMMENT '消耗品种类，如 dagger=短剑（与 API body.kind 一致）',
  `quantity` INT NOT NULL DEFAULT 0 COMMENT '当前持有数量',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '首笔记录时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后变更时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_consumable` (`user_id`, `kind`),
  KEY `idx_user_consumables_user` (`user_id`),
  CONSTRAINT `fk_user_consumables_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户消耗品库存';

-- 检测 users 表是否仍存在旧列（v30 曾加列的环境）
SET @has_legacy := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND COLUMN_NAME = 'consumable_dagger_count'
);

-- 有旧列：把正数库存写入 user_consumables（重复执行时 ON DUPLICATE 覆盖 quantity）
SET @ins := IF(
  @has_legacy > 0,
  'INSERT INTO user_consumables (user_id, kind, quantity) SELECT id, ''dagger'', consumable_dagger_count FROM users WHERE consumable_dagger_count > 0 ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)',
  'SELECT ''skip: no users.consumable_dagger_count to migrate'' AS migration_v31_ins'
);

PREPARE stmt_ins FROM @ins;
EXECUTE stmt_ins;
DEALLOCATE PREPARE stmt_ins;

-- 有旧列：删除 users.consumable_dagger_count，避免与 user_consumables 双写
SET @drop := IF(
  @has_legacy > 0,
  'ALTER TABLE `users` DROP COLUMN `consumable_dagger_count`',
  'SELECT ''skip: users.consumable_dagger_count already absent'' AS migration_v31_drop'
);

PREPARE stmt_drop FROM @drop;
EXECUTE stmt_drop;
DEALLOCATE PREPARE stmt_drop;
