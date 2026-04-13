-- 随机匹配房：与 POST /api/rooms 创建的好友房区分，用于团团积分等规则
ALTER TABLE `room_participants`
    ADD COLUMN `random_match` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=随机匹配创建，0=好友房等';
