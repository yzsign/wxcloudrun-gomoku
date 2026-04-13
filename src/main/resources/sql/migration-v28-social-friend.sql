-- 好友申请与好友关系（对齐 gomoku-minigame/docs/friend-request-social-spec.md V1.6）
-- 依赖：users.id（BIGINT UNSIGNED）

-- ---------------------------------------------------------------------------
-- 1) 好友关系（无向边：user_low_id < user_high_id，避免重复与双向两行）
--    本期不做「删除好友」，仅插入；后续若支持删除可软删或物理删。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `social_friendship` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_low_id` BIGINT UNSIGNED NOT NULL COMMENT '无向边较小端：min(双方 users.id)，须小于 user_high_id',
    `user_high_id` BIGINT UNSIGNED NOT NULL COMMENT '无向边较大端：max(双方 users.id)',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '成为好友（写入本行）的时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_friend_pair` (`user_low_id`, `user_high_id`),
    KEY `idx_friend_user_low` (`user_low_id`),
    KEY `idx_friend_user_high` (`user_high_id`),
    CONSTRAINT `fk_social_friendship_low` FOREIGN KEY (`user_low_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_social_friendship_high` FOREIGN KEY (`user_high_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='游戏内好友关系（§6 建立关系后写入）';

-- ---------------------------------------------------------------------------
-- 2) 好友申请（每条「新发起的申请周期」一行；幂等重复 POST 不插新行，仅返回既有 PENDING）
--    唯一 pending：见下方 GENERATED + uk_pending_from_to（§6.1）
--    频控：对 (from_user_id, to_user_id) 在 24h 内「新插入行数」≤3（§6），与 idx_rate_window 配合
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `social_friend_request` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，对应接口 friendRequestId',
    `from_user_id` BIGINT UNSIGNED NOT NULL COMMENT '发起人 users.id',
    `to_user_id` BIGINT UNSIGNED NOT NULL COMMENT '被申请人 users.id',
    `status` VARCHAR(24) NOT NULL COMMENT 'PENDING=待处理；ACCEPTED/REJECTED/EXPIRED；DISMISSED=互申时由对方同意一并关闭',
    `expires_at` DATETIME(3) NOT NULL COMMENT '待处理截止时间：创建时刻 +7 天，过期任务据此置 EXPIRED',
    `resolved_at` DATETIME(3) NULL COMMENT '终态时间：同意/拒绝/过期/一并关闭时写入',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '本条申请创建时间；频控 24h/3 次按 from+to+本字段计数',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '行更新时间',
    `pending_pair_key` VARCHAR(48) AS (
        CASE
            WHEN `status` = 'PENDING' THEN CONCAT(`from_user_id`, ':', `to_user_id`)
            ELSE NULL
        END
    ) STORED COMMENT '仅 PENDING 非 NULL，与 uk_pending_from_to 保证同 (from,to) 至多一条待处理',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pending_from_to` (`pending_pair_key`),
    KEY `idx_request_to_pending` (`to_user_id`, `status`, `created_at`),
    KEY `idx_request_from_pending` (`from_user_id`, `status`, `created_at`),
    KEY `idx_request_expires` (`status`, `expires_at`),
    KEY `idx_rate_window` (`from_user_id`, `to_user_id`, `created_at`),
    CONSTRAINT `fk_social_fr_from` FOREIGN KEY (`from_user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_social_fr_to` FOREIGN KEY (`to_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='好友申请；互申两条 PENDING 时一次同意全部关闭（§4.4），非主关闭行记 DISMISSED';
