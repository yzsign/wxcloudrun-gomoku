package com.gomoku.sync.api.dto;

/**
 * GET /api/social/friend-status — 战绩弹窗按钮态（§7.1）
 */
public class FriendStatusResponse {

    private boolean friends;
    /** 我向对方发起的待处理申请 */
    private boolean outgoingPending;
    private Long outgoingFriendRequestId;
    /** 对方向我发起的待处理申请（弹框处理；按钮仍可为「添加好友」） */
    private boolean incomingPending;
    private Long incomingFriendRequestId;

    public boolean isFriends() {
        return friends;
    }

    public void setFriends(boolean friends) {
        this.friends = friends;
    }

    public boolean isOutgoingPending() {
        return outgoingPending;
    }

    public void setOutgoingPending(boolean outgoingPending) {
        this.outgoingPending = outgoingPending;
    }

    public Long getOutgoingFriendRequestId() {
        return outgoingFriendRequestId;
    }

    public void setOutgoingFriendRequestId(Long outgoingFriendRequestId) {
        this.outgoingFriendRequestId = outgoingFriendRequestId;
    }

    public boolean isIncomingPending() {
        return incomingPending;
    }

    public void setIncomingPending(boolean incomingPending) {
        this.incomingPending = incomingPending;
    }

    public Long getIncomingFriendRequestId() {
        return incomingFriendRequestId;
    }

    public void setIncomingFriendRequestId(Long incomingFriendRequestId) {
        this.incomingFriendRequestId = incomingFriendRequestId;
    }
}
