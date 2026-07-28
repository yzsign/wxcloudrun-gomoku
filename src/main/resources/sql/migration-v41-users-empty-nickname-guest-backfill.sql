-- 真人用户 nickname 为空时补 5～7 位字母数字（由 id+openid 确定性生成，与客户端随机游客名规则一致）。
-- 可重复执行：已有非空昵称的行不受影响。
UPDATE `users`
SET `nickname` = LOWER(
  SUBSTRING(
    SHA2(CONCAT('gomoku-guest-v41:', `id`, ':', `openid`), 256),
    1,
    5 + (`id` % 3)
  )
)
WHERE `is_bot` = 0
  AND (`nickname` IS NULL OR TRIM(`nickname`) = '');
