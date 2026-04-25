package com.gomoku.sync.api.dto;

/**
 * GET /api/social/friends 单行；online、inGame 在 Service 层填充。
 */
public class FriendListItemDto {

    private long peerUserId;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private String remark;
    private boolean online;
    /** 是否在未结束对局中且棋手侧仍挂在 Gomoku 房间 WS（与 online 正交） */
    private boolean inGame;
    private String displayName;

    public long getPeerUserId() {
        return peerUserId;
    }

    public void setPeerUserId(long peerUserId) {
        this.peerUserId = peerUserId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isInGame() {
        return inGame;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
