-- 人机账号单独配置棋风；NULL 时每局随机一种 BotAiStyle

ALTER TABLE `users`
  ADD COLUMN `bot_ai_style` TINYINT UNSIGNED NULL COMMENT 'BotAiStyle.ordinal，NULL=每局随机' AFTER `bot_search_depth_max`;
