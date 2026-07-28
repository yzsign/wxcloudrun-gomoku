-- v42：人机 user_rating 按段位分档（12 档均匀分布）
-- 用途：随机匹配 fallback 按段位带 SELECT 人机时，资料（elo / 称号）与房主段位一致或接近。
-- 可重复执行：同一 users.id 始终映射到同一档位（MOD(id, 12)）。
-- 依赖：user_rating 表（migration-v38）、is_bot 人机（migration-v3 等）。

INSERT INTO `user_rating` (`user_id`, `elo_score`, `title_name`)
SELECT
    u.`id`,
    CASE MOD(u.`id`, 12)
        WHEN 0 THEN LEAST(999, 860 + MOD(u.`id`, 140))
        WHEN 1 THEN 1000 + MOD(u.`id`, 200)
        WHEN 2 THEN 1200 + MOD(u.`id`, 200)
        WHEN 3 THEN 1400 + MOD(u.`id`, 200)
        WHEN 4 THEN 1600 + MOD(u.`id`, 200)
        WHEN 5 THEN 1800 + MOD(u.`id`, 200)
        WHEN 6 THEN 2000 + MOD(u.`id`, 200)
        WHEN 7 THEN 2200 + MOD(u.`id`, 150)
        WHEN 8 THEN 2350 + MOD(u.`id`, 150)
        WHEN 9 THEN 2500 + MOD(u.`id`, 200)
        WHEN 10 THEN 2700 + MOD(u.`id`, 200)
        ELSE 2900 + MOD(u.`id`, 250)
    END AS `elo_score`,
    CASE MOD(u.`id`, 12)
        WHEN 0 THEN '木野狐'
        WHEN 1 THEN '石枰客'
        WHEN 2 THEN '玄素生'
        WHEN 3 THEN '落子星'
        WHEN 4 THEN '通幽手'
        WHEN 5 THEN '坐照客'
        WHEN 6 THEN '入神师'
        WHEN 7 THEN '玉楸子'
        WHEN 8 THEN '璇玑使'
        WHEN 9 THEN '天元君'
        WHEN 10 THEN '无极圣'
        ELSE '棋鬼王'
    END AS `title_name`
FROM `users` u
WHERE u.`is_bot` = 1
ON DUPLICATE KEY UPDATE
    `elo_score` = VALUES(`elo_score`),
    `title_name` = VALUES(`title_name`);
