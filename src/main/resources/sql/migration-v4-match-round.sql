-- 同房间「再来一局」多局结算：games 由按 room_id 唯一改为 (room_id, match_round)

ALTER TABLE `games`
  ADD COLUMN `match_round` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '同房间局次，首局 1' AFTER `room_id`;

ALTER TABLE `games` DROP INDEX `uk_room_id`;

ALTER TABLE `games` ADD UNIQUE KEY `uk_room_match` (`room_id`, `match_round`);
