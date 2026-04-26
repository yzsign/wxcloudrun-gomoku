package com.gomoku.sync.service;

import com.gomoku.sync.api.dto.SilentLoginRequest;
import com.gomoku.sync.api.dto.SilentLoginResponse;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.mapper.UserMapper;
import com.gomoku.sync.wx.Code2SessionWxResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 静默登录与用户资料（昵称、头像、性别）持久化逻辑测试（不访问真实微信接口）。
 */
@ExtendWith(MockitoExtension.class)
class SilentAuthServiceTest {

    @Mock
    private WeChatMiniappClient weChatMiniappClient;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SessionJwtService sessionJwtService;
    @Mock
    private AdminTokenService adminTokenService;

    @InjectMocks
    private SilentAuthService silentAuthService;

    private static Code2SessionWxResponse okWx() {
        Code2SessionWxResponse wx = new Code2SessionWxResponse();
        wx.setErrcode(0);
        wx.setOpenid("test-openid");
        wx.setUnionid("test-union");
        return wx;
    }

    @BeforeEach
    void jwtStub() {
        when(sessionJwtService.createToken(anyLong())).thenReturn("jwt-test");
        when(adminTokenService.isOpenidInAdminWhitelist(anyString())).thenReturn(false);
    }

    @Test
    void newUser_persistsNicknameAvatarGender() throws Exception {
        when(weChatMiniappClient.code2Session("code-1")).thenReturn(okWx());
        when(userMapper.selectByOpenid("test-openid")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(100L);
            return 1;
        });

        SilentLoginRequest req = new SilentLoginRequest();
        req.setCode("code-1");
        req.setNickname("玩家甲");
        req.setAvatarUrl("https://thirdwx.qlogo.cn/mmopen/xxx/0");
        req.setGender(2);

        SilentLoginResponse res = silentAuthService.silentLogin(req);

        assertEquals(100L, res.getUserId());
        assertEquals("jwt-test", res.getSessionToken());

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(cap.capture());
        verify(userMapper).insertDefaultRatingProfile(100L);
        User inserted = cap.getValue();
        assertEquals("test-openid", inserted.getOpenid());
        assertEquals("test-union", inserted.getUnionid());
        assertEquals("玩家甲", inserted.getNickname());
        assertEquals("https://thirdwx.qlogo.cn/mmopen/xxx/0", inserted.getAvatarUrl());
        assertEquals(2, inserted.getGender());
        verify(userMapper, never()).updateByOpenid(any());
    }

    @Test
    void existingUser_updatesNicknameAvatarGender() throws Exception {
        when(weChatMiniappClient.code2Session(anyString())).thenReturn(okWx());
        User existing = new User();
        existing.setId(5L);
        existing.setOpenid("test-openid");
        existing.setNickname("旧名");
        existing.setAvatarUrl("http://old");
        existing.setGender(1);
        when(userMapper.selectByOpenid("test-openid")).thenReturn(existing);

        SilentLoginRequest req = new SilentLoginRequest();
        req.setCode("x");
        req.setNickname("新名");
        req.setAvatarUrl("http://new-av");
        req.setGender(2);

        silentAuthService.silentLogin(req);

        verify(userMapper, never()).insert(any());
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateByOpenid(cap.capture());
        User updated = cap.getValue();
        assertEquals("新名", updated.getNickname());
        assertEquals("http://new-av", updated.getAvatarUrl());
        assertEquals(2, updated.getGender());
    }

    @Test
    void existingUser_nullNicknameDoesNotOverwrite() throws Exception {
        when(weChatMiniappClient.code2Session("c")).thenReturn(okWx());
        User existing = new User();
        existing.setId(5L);
        existing.setOpenid("test-openid");
        existing.setNickname("保留");
        when(userMapper.selectByOpenid("test-openid")).thenReturn(existing);

        SilentLoginRequest req = new SilentLoginRequest();
        req.setCode("c");
        req.setNickname(null);
        req.setAvatarUrl(null);

        silentAuthService.silentLogin(req);

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateByOpenid(cap.capture());
        assertEquals("保留", cap.getValue().getNickname());
    }

    @Test
    void invalidGenderIgnored() throws Exception {
        when(weChatMiniappClient.code2Session("c2")).thenReturn(okWx());
        when(userMapper.selectByOpenid("test-openid")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return 1;
        });

        SilentLoginRequest req = new SilentLoginRequest();
        req.setCode("c2");
        req.setNickname("a");
        req.setGender(99);

        silentAuthService.silentLogin(req);

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(cap.capture());
        verify(userMapper).insertDefaultRatingProfile(1L);
        assertNull(cap.getValue().getGender());
    }
}
