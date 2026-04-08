package com.gomoku.sync.api.dto;

/**
 * 静默登录：小程序 wx.login 得到的 code；昵称与头像为可选（需用户主动授权后由前端传入）
 */
public class SilentLoginRequest {

    private String code;
    private String nickname;
    private String avatarUrl;
    /** 微信 userInfo.gender：0 未知 1 男 2 女（可选，上报后写入 users.gender） */
    private Integer gender;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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
}
