package com.gomoku.sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "wx.miniapp")
public class WxMiniappProperties {

    /**
     * 小程序 AppID
     */
    private String appId = "";

    /**
     * 小程序 AppSecret（勿提交到仓库，用环境变量）
     */
    private String appSecret = "";

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }
}
