-- 好友备注（单向：owner 看 peer）；与 gomoku-minigame/docs/friend-list-home-spec.md 对齐
-- 依赖：users.id

CREATE TABLE IF NOT EXISTS `social_friend_remark` (
    `owner_user_id` BIGINT UNSIGNED NOT NULL COMMENT '设置备注的一方',
    `peer_user_id` BIGINT UNSIGNED NOT NULL COMMENT '被备注的好友 users.id',
    `remark` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '展示名；空则用对方昵称',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`owner_user_id`, `peer_user_id`),
    KEY `idx_sfr_peer` (`peer_user_id`),
    CONSTRAINT `fk_sfr_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_sfr_peer` FOREIGN KEY (`peer_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='好友单向备注';
