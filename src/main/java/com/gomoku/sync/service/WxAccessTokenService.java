package com.gomoku.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomoku.sync.config.WxMiniappProperties;
import com.gomoku.sync.wx.ClientCredentialTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * 小程序 client_credential access_token，带内存缓存与过期前刷新。
 */
@Service
public class WxAccessTokenService {

    private static final Logger log = LoggerFactory.getLogger(WxAccessTokenService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WxMiniappProperties wx;

    private final Object lock = new Object();
    private volatile String cachedToken;
    /** 建议刷新时间点（早于微信 expires_in 截止） */
    private volatile long refreshAfterEpochMs;

    public WxAccessTokenService(RestTemplate restTemplate, ObjectMapper objectMapper, WxMiniappProperties wx) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.wx = wx;
    }

    /** 强制下次重新拉取（如 msgSecCheck 返回 40001） */
    public void invalidate() {
        synchronized (lock) {
            cachedToken = null;
            refreshAfterEpochMs = 0;
        }
    }

    public String getAccessToken() throws IOException {
        if (!StringUtils.hasText(wx.getAppId()) || !StringUtils.hasText(wx.getAppSecret())) {
            throw new IllegalStateException("未配置 wx.miniapp.app-id / app-secret，无法获取 access_token");
        }
        long now = System.currentTimeMillis();
        synchronized (lock) {
            if (cachedToken != null && now < refreshAfterEpochMs) {
                return cachedToken;
            }
            refreshLocked();
            return cachedToken;
        }
    }

    private void refreshLocked() throws IOException {
        String url =
                UriComponentsBuilder.fromHttpUrl("https://api.weixin.qq.com/cgi-bin/token")
                        .queryParam("grant_type", "client_credential")
                        .queryParam("appid", wx.getAppId())
                        .queryParam("secret", wx.getAppSecret())
                        .build(true)
                        .toUriString();
        String body = restTemplate.getForObject(url, String.class);
        if (body == null) {
            throw new IOException("微信 access_token 接口无响应");
        }
        ClientCredentialTokenResponse r = objectMapper.readValue(body, ClientCredentialTokenResponse.class);
        if (r.getErrcode() != null && r.getErrcode() != 0) {
            throw new IOException("access_token 失败: errcode=" + r.getErrcode() + " " + r.getErrmsg());
        }
        if (!StringUtils.hasText(r.getAccessToken())) {
            throw new IOException("access_token 响应缺少 access_token");
        }
        int sec = r.getExpiresIn() != null ? r.getExpiresIn() : 7200;
        cachedToken = r.getAccessToken();
        long safetyMs = Math.min(300_000L, sec * 500L);
        refreshAfterEpochMs = System.currentTimeMillis() + sec * 1000L - safetyMs;
        log.debug("wx access_token refreshed, expires_in={}s", sec);
    }
}
