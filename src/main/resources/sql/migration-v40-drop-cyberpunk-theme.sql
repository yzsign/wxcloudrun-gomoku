-- 赛博主题已下线：装备槽 THEME 统一回退檀木
UPDATE user_equipped_cosmetics
SET item_id = 'classic'
WHERE category = 'THEME'
  AND item_id = 'cyberpunk';
