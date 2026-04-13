package com.gomoku.sync.api.dto;

/**
 * 与 friend-request-social-spec §6.1 响应约定一致。
 */
public class CreateFriendResponse {

    /** CREATED | PENDING | ALREADY_FRIENDS | RATE_LIMITED */
    private String status;
    private Long friendRequestId;

    public CreateFriendResponse() {
    }

    public CreateFriendResponse(String status, Long friendRequestId) {
        this.status = status;
        this.friendRequestId = friendRequestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getFriendRequestId() {
        return friendRequestId;
    }

    public void setFriendRequestId(Long friendRequestId) {
        this.friendRequestId = friendRequestId;
    }
}
