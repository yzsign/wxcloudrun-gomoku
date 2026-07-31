-- 移除示例商品「云子」（若不需要可在库中执行；shop_item_prices 随外键 CASCADE 删除）
DELETE FROM shop_items WHERE item_code = 'yunzi';
