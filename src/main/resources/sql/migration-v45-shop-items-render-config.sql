-- v45：商品外观配置（棋子皮肤渐变/贴图 URL），供小程序动态加载，无需改 themes.js 发版
ALTER TABLE `shop_items`
  ADD COLUMN `render_config` JSON NULL COMMENT '客户端绘制配置：gradient 渐变色或 texture 贴图 URL' AFTER `client_row_id`;
