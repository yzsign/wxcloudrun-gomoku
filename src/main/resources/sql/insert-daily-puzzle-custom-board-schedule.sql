-- 为「链形延伸」绑定排期（单条语句，不依赖 LAST_INSERT_ID / 事务）。
-- 请先执行 insert-daily-puzzle-custom-board.sql。
-- 将下方日期改为上线日（Asia/Shanghai）；若题库中有多条同名 title，会取 id 最大的一条。

INSERT INTO daily_puzzle_schedule (puzzle_date, puzzle_id)
SELECT '2026-04-10', id
FROM daily_puzzle
WHERE title = '链形延伸'
ORDER BY id DESC
LIMIT 1
ON DUPLICATE KEY UPDATE puzzle_id = VALUES(puzzle_id);
