-- 人机棋风：与 BotAiStyle 枚举 ordinal 对应（0 均衡 1 进攻 2 防守 3 多变），NULL 表示由 white_user_id 推导

ALTER TABLE `room_participants`
  ADD COLUMN `bot_ai_style` TINYINT UNSIGNED NULL COMMENT '人机白方棋风 ordinal，NULL 见 BotAiStyle.forBotUserId' AFTER `bot_search_depth_max`;
