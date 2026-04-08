-- v16：若曾执行过「在 users 上增加 piece_skin_qingtao_libai_unlocked」的旧脚本，将数据迁入 user_piece_skin_unlocks 后删列。
-- 需先执行 migration-v15-user-piece-skin-unlocks.sql（表已存在）。
-- 可重复执行。

SET @col_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND COLUMN_NAME = 'piece_skin_qingtao_libai_unlocked'
);

-- 旧列存在时：把已解锁用户写入解锁表（避免重复：主键冲突则忽略）
SET @sql_migrate := IF(
  @col_exists > 0,
  'INSERT IGNORE INTO `user_piece_skin_unlocks` (`user_id`, `skin_id`, `unlock_source`, `points_spent`)
   SELECT `id`, ''qingtao_libai'', ''MIGRATION_FROM_USERS_COLUMN'', 0
   FROM `users` WHERE `piece_skin_qingtao_libai_unlocked` = 1',
  'SELECT ''skip: no users.piece_skin_qingtao_libai_unlocked column'' AS migration_v16_migrate'
);
PREPARE stmt FROM @sql_migrate;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_drop := IF(
  @col_exists > 0,
  'ALTER TABLE `users` DROP COLUMN `piece_skin_qingtao_libai_unlocked`',
  'SELECT ''skip: piece_skin_qingtao_libai_unlocked column already absent'' AS migration_v16_drop'
);
PREPARE stmt FROM @sql_drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
