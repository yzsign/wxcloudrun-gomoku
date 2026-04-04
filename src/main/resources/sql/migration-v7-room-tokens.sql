-- 房间 token 与多实例：room_participants 持久化 black_token / white_token，供任意实例从 DB 恢复 GameRoom 元数据

ALTER TABLE `room_participants`
  ADD COLUMN `black_token` VARCHAR(64) NULL COMMENT '黑方 WebSocket token' AFTER `black_user_id`,
  ADD COLUMN `white_token` VARCHAR(64) NULL COMMENT '白方 WebSocket token' AFTER `white_user_id`,
  ADD COLUMN `white_is_bot` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '白方是否为人机' AFTER `white_token`,
  ADD COLUMN `bot_search_depth_min` INT NULL COMMENT '人机白方搜索深度下限' AFTER `white_is_bot`,
  ADD COLUMN `bot_search_depth_max` INT NULL COMMENT '人机白方搜索深度上限' AFTER `bot_search_depth_min`;
