package com.gomoku.sync.domain;

/**
 * 杂货铺装备种类：每用户在 {@code user_equipped_cosmetics} 中每个 category 至多一行。
 * 新增种类时在此与客户端约定字符串常量即可。
 */
public final class CosmeticCategory {

    public static final String PIECE_SKIN = "PIECE_SKIN";
    /** 界面棋盘主题（檀木/青瓷/水墨），与客户端 themes.THEMES id 一致 */
    public static final String THEME = "THEME";

    private CosmeticCategory() {}
}
