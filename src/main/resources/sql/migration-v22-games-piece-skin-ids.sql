-- v22：对局归档记录终局时双方棋子皮肤，供棋谱回放展示（与 users.piece_skin_id 取值域一致）
ALTER TABLE `games`
  ADD COLUMN `black_piece_skin_id` VARCHAR(32) NULL DEFAULT NULL COMMENT '终局黑方棋子皮肤' AFTER `moves_json`,
  ADD COLUMN `white_piece_skin_id` VARCHAR(32) NULL DEFAULT NULL COMMENT '终局白方棋子皮肤' AFTER `black_piece_skin_id`;
