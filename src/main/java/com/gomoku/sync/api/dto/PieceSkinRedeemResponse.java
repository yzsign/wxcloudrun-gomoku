package com.gomoku.sync.api.dto;

import java.util.Collections;
import java.util.List;

/**
 * POST /api/me/piece-skins/redeem 成功响应。
 */
public class PieceSkinRedeemResponse {

    private final int activityPoints;
    private final List<String> pieceSkinUnlockedIds;
    private final boolean alreadyOwned;

    public PieceSkinRedeemResponse(int activityPoints, List<String> pieceSkinUnlockedIds, boolean alreadyOwned) {
        this.activityPoints = activityPoints;
        this.pieceSkinUnlockedIds = pieceSkinUnlockedIds != null ? pieceSkinUnlockedIds : Collections.emptyList();
        this.alreadyOwned = alreadyOwned;
    }

    public int getActivityPoints() {
        return activityPoints;
    }

    public List<String> getPieceSkinUnlockedIds() {
        return pieceSkinUnlockedIds;
    }

    public boolean isAlreadyOwned() {
        return alreadyOwned;
    }
}
