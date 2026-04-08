-- v19：当前佩戴的棋子皮肤（与客户端 piece skin id 一致，如 basic / tuan_moe / qingtao_libai）
ALTER TABLE `users`
  ADD COLUMN `piece_skin_id` VARCHAR(32) NULL DEFAULT NULL COMMENT '当前佩戴棋子皮肤 id，与客户端一致' AFTER `gender`;
