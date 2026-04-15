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

    /**
     * 是否对聊天等文本调用微信 msg_sec_check（需 app-id / app-secret）
     */
    private boolean msgSecCheckEnabled = true;

    /**
     * msg_sec_check 调用失败（网络、非 token 类 errcode）时是否放行发送
     */
    private boolean msgSecCheckFailOpenOnError = true;

    /**
     * 场景：1 资料 2 评论 3 论坛 4 社交日志（对局聊天建议 4）
     */
    private int msgSecCheckScene = 4;

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

    public boolean isMsgSecCheckEnabled() {
        return msgSecCheckEnabled;
    }

    public void setMsgSecCheckEnabled(boolean msgSecCheckEnabled) {
        this.msgSecCheckEnabled = msgSecCheckEnabled;
    }

    public boolean isMsgSecCheckFailOpenOnError() {
        return msgSecCheckFailOpenOnError;
    }

    public void setMsgSecCheckFailOpenOnError(boolean msgSecCheckFailOpenOnError) {
        this.msgSecCheckFailOpenOnError = msgSecCheckFailOpenOnError;
    }

    public int getMsgSecCheckScene() {
        return msgSecCheckScene;
    }

    public void setMsgSecCheckScene(int msgSecCheckScene) {
        this.msgSecCheckScene = msgSecCheckScene;
    }
}
