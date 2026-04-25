package com.gomoku.sync.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GET /api/social/friends 单行；online、inGame、spectating 在 Service 层填充。
 * <p>online 为真表示好友可感知为「在线」：已建立用户推送 /ws/user，或正挂在未终局对局的 Gomoku 房间链路上（与 inGame / spectating 为真时同时成立）。
 */
public class FriendListItemDto {

    private long peerUserId;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private String remark;
    /** 用户级 WS 在线，或正参与未终局对局（见 class 说明） */
    private boolean online;
    /** 是否在未结束对局中且棋手侧仍挂在 Gomoku 房间 WS */
    private boolean inGame;
    /** 是否以观战身份挂在某未终局房间的 Gomoku WS 上（与 inGame 互斥） */
    @JsonProperty("spectating")
    private boolean spectating;
    private String displayName;
    /** 天梯分 */
    private Integer eloScore;
    /** 称号，与 {@code users.title_name} 一致，随对局结算与分数同步 */
    private String titleName;

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

    public boolean isSpectating() {
        return spectating;
    }

    public void setSpectating(boolean spectating) {
        this.spectating = spectating;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getEloScore() {
        return eloScore;
    }

    public void setEloScore(Integer eloScore) {
        this.eloScore = eloScore;
    }

    public String getTitleName() {
        return titleName;
    }

    public void setTitleName(String titleName) {
        this.titleName = titleName;
    }
}
