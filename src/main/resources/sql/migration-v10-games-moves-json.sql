-- 对局回放：归档终局手顺（JSON 数组 [{r,c,color}, ...]）
ALTER TABLE `games`
  ADD COLUMN `moves_json` LONGTEXT NULL COMMENT '终局手顺 JSON，与 room_game_state 中 moves 结构一致' AFTER `white_elo_delta`;
