-- 运营后台独立管理员账号（用户名密码登录，与微信 users 表分离）
CREATE TABLE IF NOT EXISTS `admin_operators` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(64) NOT NULL COMMENT '登录用户名，唯一',
  `password_hash` VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码哈希',
  `display_name` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '展示名称',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
  `last_login_at` DATETIME NULL DEFAULT NULL COMMENT '最近登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_operators_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营后台管理员';
