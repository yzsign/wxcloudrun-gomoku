-- 人机账号：每人固定搜索深度区间，对局内每步在 [min,max] 随机（与 GomokuAiEngine.chooseMove depth 一致）
ALTER TABLE `users`
  ADD COLUMN `bot_search_depth_min` INT UNSIGNED NOT NULL DEFAULT 2 COMMENT '人机：minimax 深度下限（含）' AFTER `is_bot`,
  ADD COLUMN `bot_search_depth_max` INT UNSIGNED NOT NULL DEFAULT 4 COMMENT '人机：minimax 深度上限（含）' AFTER `bot_search_depth_min`;

-- 100 名预设人机：执行 migration-v6-bot-depth-assign.sql 为每人写入不同区间。
-- 亦可按需单条更新，例如：
-- UPDATE users SET bot_search_depth_min = 1, bot_search_depth_max = 2 WHERE is_bot = 1 AND id = ?;
