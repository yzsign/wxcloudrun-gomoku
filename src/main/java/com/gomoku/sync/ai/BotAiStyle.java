package com.gomoku.sync.ai;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机匹配人机棋风：影响评估权重、候选排序；部分风格在根节点从若干相近最优解中随机，增加变化。
 */
public enum BotAiStyle {
    /** 与历史默认一致 */
    BALANCED(1.0, 1.0, 1.0, 1.0, false, 0),
    /** 偏重进攻与连片 */
    AGGRESSIVE(1.14, 0.94, 1.2, 0.86, false, 0),
    /** 偏重挡对方与消势 */
    DEFENSIVE(0.94, 1.14, 0.86, 1.2, false, 0),
    /** 在若干分差内的最优解中随机，棋路更不可预测 */
    CREATIVE(1.0, 1.0, 1.0, 1.0, true, 12_000);

    private final double orderMyW;
    private final double orderOppW;
    private final double evalMyW;
    private final double evalOppW;
    private final boolean rootRandomAmongNearBest;
    private final int nearBestScoreMargin;

    BotAiStyle(
            double orderMyW,
            double orderOppW,
            double evalMyW,
            double evalOppW,
            boolean rootRandomAmongNearBest,
            int nearBestScoreMargin) {
        this.orderMyW = orderMyW;
        this.orderOppW = orderOppW;
        this.evalMyW = evalMyW;
        this.evalOppW = evalOppW;
        this.rootRandomAmongNearBest = rootRandomAmongNearBest;
        this.nearBestScoreMargin = nearBestScoreMargin;
    }

    public double getOrderMyW() {
        return orderMyW;
    }

    public double getOrderOppW() {
        return orderOppW;
    }

    public double getEvalMyW() {
        return evalMyW;
    }

    public double getEvalOppW() {
        return evalOppW;
    }

    public boolean isRootRandomAmongNearBest() {
        return rootRandomAmongNearBest;
    }

    public int getNearBestScoreMargin() {
        return nearBestScoreMargin;
    }

    /** 按人机账号 id 稳定映射到一种风格，使不同机器人个性固定、可区分。 */
    public static BotAiStyle forBotUserId(long botUserId) {
        BotAiStyle[] v = values();
        return v[(int) Math.floorMod(botUserId, (long) v.length)];
    }

    public static BotAiStyle fromOrdinal(int ordinal) {
        BotAiStyle[] v = values();
        if (ordinal < 0 || ordinal >= v.length) {
            return BALANCED;
        }
        return v[ordinal];
    }

    /** 在若干种风格中均匀随机（接入人机、且未显式配置 {@code users.bot_ai_style} 时使用）。 */
    public static int randomOrdinal() {
        return ThreadLocalRandom.current().nextInt(values().length);
    }

    /**
     * {@code users.bot_ai_style} 有值时用之（非法 ordinal 会回落到 BALANCED）；为 {@code null} 时每局随机一种风格。
     */
    public static int resolveOrdinal(Integer botAiStyleFromDb) {
        if (botAiStyleFromDb != null) {
            return fromOrdinal(botAiStyleFromDb).ordinal();
        }
        return randomOrdinal();
    }
}
