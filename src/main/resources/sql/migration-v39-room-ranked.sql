-- 好友房是否计入天梯：随机匹配始终 ranked=1；POST /api/rooms 创建的好友房为 ranked=0（休闲房）
ALTER TABLE room_participants
    ADD COLUMN ranked TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=计入天梯与战绩统计，0=休闲房' AFTER random_match;

-- 已有好友房（非随机匹配）保持原样仍计分；新创建的好友房由应用写入 ranked=0
