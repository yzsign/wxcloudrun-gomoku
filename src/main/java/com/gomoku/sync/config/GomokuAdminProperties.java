package com.gomoku.sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 管理端：{@code X-Admin-Token} 运维令牌，和/或 微信用户 openid 白名单（与 Bearer 会话联用）。
 */
@ConfigurationProperties(prefix = "gomoku.admin")
public class GomokuAdminProperties {

    /**
     * 建议通过环境变量 {@code GOMOKU_ADMIN_TOKEN} 注入长随机串（脚本/Postman）。
     */
    private String token = "";

    /**
     * 英文逗号分隔的微信 openid；与 {@code Authorization: Bearer sessionToken} 联用时视为管理员。
     * 环境变量 {@code GOMOKU_ADMIN_OPENIDS}。
     */
    private String openids = "";

    private volatile Set<String> openidSetCache;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getOpenids() {
        return openids;
    }

    public void setOpenids(String openids) {
        this.openids = openids;
        this.openidSetCache = null;
    }

    public Set<String> getOpenidSet() {
        if (openidSetCache != null) {
            return openidSetCache;
        }
        if (openids == null || openids.trim().isEmpty()) {
            openidSetCache = Collections.emptySet();
            return openidSetCache;
        }
        LinkedHashSet<String> s = new LinkedHashSet<>();
        for (String part : openids.split(",")) {
            if (part == null) {
                continue;
            }
            String t = part.trim();
            if (!t.isEmpty()) {
                s.add(t);
            }
        }
        openidSetCache = Collections.unmodifiableSet(s);
        return openidSetCache;
    }

    /** 配置了 token 或 openid 白名单之一即启用管理接口。 */
    public boolean isEnabled() {
        return (token != null && !token.isEmpty()) || !getOpenidSet().isEmpty();
    }
}
