package com.gomoku.sync.domain;

/**
 * 杂货铺装备种类：每用户在 {@code user_equipped_cosmetics} 中每个 category 至多一行。
 * 新增种类时在此与客户端约定字符串常量即可。
 */
public final class CosmeticCategory {

    public static final String PIECE_SKIN = "PIECE_SKIN";
    /** 界面棋盘主题（檀木/青瓷/水墨），与客户端 themes.THEMES id 一致 */
    public static final String THEME = "THEME";
    /**
     * 对局头像旁技能槽（当前仅短剑 Q），item_id 与消耗品 kind 一致为 {@code dagger}；
     * 卸下时删除槽位行（客户端 POST equip itemId={@code off}）。
     */
    public static final String BOARD_SKILL = "BOARD_SKILL";

    private CosmeticCategory() {}
}
