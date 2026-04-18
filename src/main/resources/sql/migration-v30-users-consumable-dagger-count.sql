-- 已废弃：短剑库存迁至 user_consumables（见 migration-v31），勿在 users 表增加 consumable_dagger_count。
-- 保留此文件以免与旧部署脚本名称混淆；执行时无表结构变更。
SELECT 'deprecated: consumables use user_consumables (migration-v31)' AS migration_v30_note;
