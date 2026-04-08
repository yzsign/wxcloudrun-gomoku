-- 仅在 users 中尚不存在 activity_points 时添加（可重复执行，不会因重复列报错）
-- 若已存在该列，会输出提示行，不修改表结构。

SET @exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND COLUMN_NAME = 'activity_points'
);

SET @sql := IF(
  @exists = 0,
  'ALTER TABLE `users` ADD COLUMN `activity_points` INT NOT NULL DEFAULT 0 COMMENT ''活跃积分（签到/任务等，与 Elo 无关）''',
  'SELECT ''skip: activity_points already exists'' AS migration_v14'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
