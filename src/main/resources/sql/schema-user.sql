-- 微信小游戏用户：主表 users（资料 + 人机配置）与 user_rating（天梯与统计，rule.md）分离；新库一键建表
-- 已有库从旧版 users 单表升级请执行 migration-v38-users-split-user-rating.sql

CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `openid` VARCHAR(64) NOT NULL COMMENT '微信小游戏 openid，唯一登录标识',
  `is_bot` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=人机账号',
  `bot_search_depth_min` INT UNSIGNED NOT NULL DEFAULT 2 COMMENT '人机：minimax 深度下限（含）',
  `bot_search_depth_max` INT UNSIGNED NOT NULL DEFAULT 4 COMMENT '人机：minimax 深度上限（含）',
  `bot_ai_style` TINYINT UNSIGNED NULL COMMENT '人机棋风 BotAiStyle.ordinal，NULL=每局随机',
  `unionid` VARCHAR(64) DEFAULT NULL COMMENT '微信 unionid，跨应用唯一；未绑定或未上报为 NULL',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '用户昵称（微信资料或客户端展示名）',
  `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像 URL',
  `gender` TINYINT UNSIGNED NULL DEFAULT NULL COMMENT '微信 gender：0未知 1男 2女；NULL=从未上报',
  `piece_skin_id` VARCHAR(32) NULL DEFAULT NULL COMMENT '当前佩戴棋子皮肤 id（与 user_equipped_cosmetics.PIECE_SKIN 同步）',
  `last_login_at` DATETIME(3) DEFAULT NULL COMMENT '最近一次登录时间',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '记录创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '记录最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_is_bot` (`is_bot`),
  KEY `idx_unionid` (`unionid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_rating` (
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT 'users.id，1:1',
  `elo_score` INT NOT NULL DEFAULT 1200 COMMENT '天梯分',
  `title_name` VARCHAR(32) NULL DEFAULT NULL COMMENT '称号，与 rule.md §5 一致，随对局结算更新',
  `activity_points` INT NOT NULL DEFAULT 0 COMMENT '活跃积分（团团积分），与天梯分独立，见 rule.md §12',
  `consecutive_wins` INT NOT NULL DEFAULT 0 COMMENT '连胜场数；负局或和棋后归零',
  `consecutive_losses` INT NOT NULL DEFAULT 0 COMMENT '连败场数',
  `today_net_change` INT NOT NULL DEFAULT 0 COMMENT '当日天梯净变动累计，用于单日变动上限（与 elo_carry_over 配合）',
  `elo_carry_over` INT NOT NULL DEFAULT 0 COMMENT '单日净变动溢出，次日并入 elo_score',
  `last_rating_reset_date` DATE DEFAULT NULL COMMENT '日切日期：单日净变动统计与重置所依据的「当前日」',
  `runaway_count` INT NOT NULL DEFAULT 0 COMMENT '累计逃跑次数（作逃跑方）',
  `total_games` INT NOT NULL DEFAULT 0 COMMENT '已结算对局总局数',
  `win_count` INT NOT NULL DEFAULT 0 COMMENT '胜局数',
  `draw_count` INT NOT NULL DEFAULT 0 COMMENT '和棋局数',
  `season_end_score` INT DEFAULT NULL COMMENT '赛季末备份的天梯分（elo_score 快照）',
  `placement_fair_games` INT NOT NULL DEFAULT 0 COMMENT '无逃跑完成局数（定级）',
  `newbie_match_games` INT NOT NULL DEFAULT 0 COMMENT '§6.3 已结算局数',
  `newbie_runaway_tally` INT NOT NULL DEFAULT 0 COMMENT '保护期内作逃跑方次数',
  `low_trust` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=低信誉（rule.md §7.4）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '本行统计最后更新时间',
  PRIMARY KEY (`user_id`),
  KEY `idx_elo_score` (`elo_score`),
  CONSTRAINT `fk_user_rating_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
