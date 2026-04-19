-- =============================================================================
-- v34：user_consumables (user_id, kind) 联合唯一索引（可重复执行）
-- =============================================================================
-- 用途：/api/me/consumables/use、兑换等按 user_id + kind 行锁与等值查询。
-- 标准部署在 v31 已含 uk_user_consumable；本迁移仅在缺失时补建，已存在则跳过。
-- =============================================================================

SET @db := DATABASE();

SELECT COUNT(*) INTO @tbl
FROM information_schema.tables
WHERE table_schema = @db AND table_name = 'user_consumables';

SELECT COUNT(*) INTO @has_uk
FROM information_schema.statistics
WHERE table_schema = @db AND table_name = 'user_consumables'
  AND index_name = 'uk_user_consumable';

SET @ddl := IF(
  @tbl > 0 AND @has_uk = 0,
  'ALTER TABLE `user_consumables` ADD UNIQUE KEY `uk_user_consumable` (`user_id`, `kind`)',
  'SELECT ''v34 skip: user_consumables missing or uk_user_consumable already exists'' AS migration_v34_note'
);

PREPARE stmt_v34 FROM @ddl;
EXECUTE stmt_v34;
DEALLOCATE PREPARE stmt_v34;
