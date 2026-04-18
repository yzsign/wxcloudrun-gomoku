-- v31：消耗品独立表 user_consumables；若历史上在 users 上存在 consumable_dagger_count 则迁移后删除该列（可重复执行）

CREATE TABLE IF NOT EXISTS `user_consumables` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `kind` VARCHAR(32) NOT NULL COMMENT '种类：dagger=短剑等',
  `quantity` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_consumable` (`user_id`, `kind`),
  KEY `idx_user_consumables_user` (`user_id`),
  CONSTRAINT `fk_user_consumables_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @has_legacy := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND COLUMN_NAME = 'consumable_dagger_count'
);

SET @ins := IF(
  @has_legacy > 0,
  'INSERT INTO user_consumables (user_id, kind, quantity) SELECT id, ''dagger'', consumable_dagger_count FROM users WHERE consumable_dagger_count > 0 ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)',
  'SELECT ''skip: no users.consumable_dagger_count to migrate'' AS migration_v31_ins'
);

PREPARE stmt_ins FROM @ins;
EXECUTE stmt_ins;
DEALLOCATE PREPARE stmt_ins;

SET @drop := IF(
  @has_legacy > 0,
  'ALTER TABLE `users` DROP COLUMN `consumable_dagger_count`',
  'SELECT ''skip: users.consumable_dagger_count already absent'' AS migration_v31_drop'
);

PREPARE stmt_drop FROM @drop;
EXECUTE stmt_drop;
DEALLOCATE PREPARE stmt_drop;
