-- users：持久化微信 userInfo.gender（0 未知 1 男 2 女），供战绩等接口返回对手默认头像等
ALTER TABLE `users`
    ADD COLUMN `gender` TINYINT UNSIGNED NULL DEFAULT NULL
        COMMENT '微信 gender：0未知 1男 2女；NULL=从未上报'
        AFTER `avatar_url`;
