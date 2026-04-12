-- 对局内聊天消息（联机房间；按 room_id 查询历史）
CREATE TABLE IF NOT EXISTS `room_chat_message` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `room_id` VARCHAR(32) NOT NULL,
    `sender_user_id` BIGINT NOT NULL,
    `kind` VARCHAR(16) NOT NULL COMMENT 'TEXT | QUICK | EMOJI',
    `content` VARCHAR(128) NOT NULL COMMENT '正文或快捷句或单个 emoji',
    `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_room_created` (`room_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- PVP 长按举报：关联某条聊天
CREATE TABLE IF NOT EXISTS `room_chat_report` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `room_id` VARCHAR(32) NOT NULL,
    `message_id` BIGINT UNSIGNED NOT NULL,
    `reporter_user_id` BIGINT NOT NULL,
    `reason` VARCHAR(255) NULL,
    `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_room_msg` (`room_id`, `message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
