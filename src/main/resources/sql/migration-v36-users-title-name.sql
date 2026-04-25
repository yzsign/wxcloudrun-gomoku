-- 用户称号：与 elo 分段一致，对局结算时由 RatingTitleUtil 随 elo 更新
ALTER TABLE users
    ADD COLUMN title_name VARCHAR(32) NULL COMMENT '称号，与 rule.md §5 / ratingTitle.js 一致' AFTER elo_score;

UPDATE users
SET title_name = CASE
                     WHEN elo_score < 1000 THEN '木野狐'
                     WHEN elo_score < 1200 THEN '石枰客'
                     WHEN elo_score < 1400 THEN '玄素生'
                     WHEN elo_score < 1600 THEN '落子星'
                     WHEN elo_score < 1800 THEN '通幽手'
                     WHEN elo_score < 2000 THEN '坐照客'
                     WHEN elo_score < 2200 THEN '入神师'
                     WHEN elo_score < 2350 THEN '玉楸子'
                     WHEN elo_score < 2500 THEN '璇玑使'
                     WHEN elo_score < 2700 THEN '天元君'
                     WHEN elo_score < 2900 THEN '无极圣'
                     ELSE '棋鬼王'
    END
WHERE title_name IS NULL;
