-- 人机账号：为尚未设置性别的行随机写入 1（男）或 2（女），便于战绩等接口返回 opponentGender
-- 仅更新 gender 为空或 0 的行；已有人为指定值的不改（可重复执行，第二次通常 0 行）
UPDATE `users`
SET `gender` = IF(RAND() < 0.5, 1, 2)
WHERE `is_bot` = 1
  AND (`gender` IS NULL OR `gender` = 0);
