package com.gomoku.sync.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 小程序静默登录响应：须同时包含 sessionToken（JWT），供联机接口校验身份。
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class SilentLoginResponse {

    private final long userId;
    /** 后续请求放在 Header：Authorization: Bearer &lt;sessionToken&gt; */
    private final String sessionToken;
    /** 当前微信 openid 是否在管理白名单（与 /api/me/admin-status 一致） */
    private final boolean admin;

    public SilentLoginResponse(long userId, String sessionToken, boolean admin) {
        this.userId = userId;
        this.sessionToken = sessionToken;
        this.admin = admin;
    }

    public long getUserId() {
        return userId;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public boolean isAdmin() {
        return admin;
    }
}
