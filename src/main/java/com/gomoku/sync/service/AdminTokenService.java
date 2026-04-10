package com.gomoku.sync.service;

import com.gomoku.sync.config.GomokuAdminProperties;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class AdminTokenService {

    public static final String HEADER = "X-Admin-Token";

    private final GomokuAdminProperties properties;
    private final UserMapper userMapper;

    public AdminTokenService(GomokuAdminProperties properties, UserMapper userMapper) {
        this.properties = properties;
        this.userMapper = userMapper;
    }

    public boolean isApiEnabled() {
        return properties.isEnabled();
    }

    /** 与请求头 {@value #HEADER} 比较（常量时间）；未配置 token 时恒为 false。 */
    public boolean matchesToken(String headerValue) {
        String expected = properties.getToken();
        if (expected == null || expected.isEmpty()) {
            return false;
        }
        String got = headerValue == null ? "" : headerValue.trim();
        if (expected.length() != got.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < expected.length(); i++) {
            r |= expected.charAt(i) ^ got.charAt(i);
        }
        return r == 0;
    }

    /** 当前用户 openid 是否在 {@link GomokuAdminProperties#getOpenidSet()} 中。 */
    public boolean isUserAdmin(long userId) {
        if (properties.getOpenidSet().isEmpty()) {
            return false;
        }
        User u = userMapper.selectById(userId);
        if (u == null || u.getOpenid() == null || u.getOpenid().isEmpty()) {
            return false;
        }
        return properties.getOpenidSet().contains(u.getOpenid());
    }

    /** 微信 openid 是否在白名单中（静默登录等场景不查库）。 */
    public boolean isOpenidInAdminWhitelist(String openid) {
        if (openid == null || openid.isEmpty()) {
            return false;
        }
        return properties.getOpenidSet().contains(openid);
    }
}
