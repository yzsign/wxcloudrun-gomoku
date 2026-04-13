package com.gomoku.sync.api.dto;

/**
 * POST /api/social/friend-requests
 */
public class CreateFriendRequestBody {

    private Long targetUserId;

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }
}
