package com.gomoku.sync.api.dto;

/**
 * 消耗品兑换或使用成功后的响应：与 GET /api/me/rating 中积分、库存对齐。
 */
public class ConsumableMutationResponse {

    private final int activityPoints;
    private final int consumableDaggerCount;
    private final int consumableLoveCount;

    public ConsumableMutationResponse(int activityPoints, int consumableDaggerCount, int consumableLoveCount) {
        this.activityPoints = activityPoints;
        this.consumableDaggerCount = consumableDaggerCount;
        this.consumableLoveCount = consumableLoveCount;
    }

    public int getActivityPoints() {
        return activityPoints;
    }

    public int getConsumableDaggerCount() {
        return consumableDaggerCount;
    }

    public int getConsumableLoveCount() {
        return consumableLoveCount;
    }
}
