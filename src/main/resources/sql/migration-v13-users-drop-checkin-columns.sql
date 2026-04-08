-- v13：从 users 删除签到相关列（仅在与 user_checkin_state 完成数据对齐后执行）
-- 若从未应用 migration-v11，则无需执行本文件。

ALTER TABLE `users`
  DROP COLUMN `checkin_last_ymd`,
  DROP COLUMN `checkin_streak`,
  DROP COLUMN `checkin_history_json`,
  DROP COLUMN `piece_skin_tuan_moe_unlocked`;
