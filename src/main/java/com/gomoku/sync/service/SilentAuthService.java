package com.gomoku.sync.service;

import com.gomoku.sync.api.dto.SilentLoginRequest;
import com.gomoku.sync.api.dto.SilentLoginResponse;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.mapper.UserMapper;
import com.gomoku.sync.wx.Code2SessionWxResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Date;

@Service
public class SilentAuthService {

    private final WeChatMiniappClient weChatMiniappClient;
    private final UserMapper userMapper;
    private final SessionJwtService sessionJwtService;

    public SilentAuthService(
            WeChatMiniappClient weChatMiniappClient,
            UserMapper userMapper,
            SessionJwtService sessionJwtService) {
        this.weChatMiniappClient = weChatMiniappClient;
        this.userMapper = userMapper;
        this.sessionJwtService = sessionJwtService;
    }

    public SilentLoginResponse silentLogin(SilentLoginRequest req) throws IOException {
        Code2SessionWxResponse wx = weChatMiniappClient.code2Session(req.getCode());
        if (wx.getErrcode() != null && wx.getErrcode() != 0) {
            String msg = wx.getErrmsg() != null ? wx.getErrmsg() : "微信登录失败";
            throw new IllegalArgumentException(msg + " (" + wx.getErrcode() + ")");
        }
        if (!StringUtils.hasText(wx.getOpenid())) {
            throw new IllegalArgumentException("未获取到 openid");
        }

        Date now = new Date();
        Integer gender = normalizeWeChatGender(req.getGender());
        User existing = userMapper.selectByOpenid(wx.getOpenid());
        if (existing == null) {
            User u = new User();
            u.setOpenid(wx.getOpenid());
            u.setUnionid(wx.getUnionid());
            u.setNickname(req.getNickname());
            u.setAvatarUrl(req.getAvatarUrl());
            if (gender != null) {
                u.setGender(gender);
            }
            u.setLastLoginAt(now);
            userMapper.insert(u);
            return new SilentLoginResponse(u.getId(), sessionJwtService.createToken(u.getId()));
        }

        if (StringUtils.hasText(wx.getUnionid())) {
            existing.setUnionid(wx.getUnionid());
        }
        if (req.getNickname() != null) {
            existing.setNickname(req.getNickname());
        }
        if (req.getAvatarUrl() != null) {
            existing.setAvatarUrl(req.getAvatarUrl());
        }
        if (gender != null) {
            existing.setGender(gender);
        }
        existing.setLastLoginAt(now);
        userMapper.updateByOpenid(existing);
        return new SilentLoginResponse(existing.getId(), sessionJwtService.createToken(existing.getId()));
    }

    /** 仅接受微信合法取值 0/1/2，其余忽略 */
    private static Integer normalizeWeChatGender(Integer g) {
        if (g == null) {
            return null;
        }
        if (g == 0 || g == 1 || g == 2) {
            return g;
        }
        return null;
    }
}
