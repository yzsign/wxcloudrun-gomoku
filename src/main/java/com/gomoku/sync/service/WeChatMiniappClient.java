package com.gomoku.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomoku.sync.config.WxMiniappProperties;
import com.gomoku.sync.wx.Code2SessionWxResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Service
public class WeChatMiniappClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WxMiniappProperties wx;

    public WeChatMiniappClient(RestTemplate restTemplate, ObjectMapper objectMapper, WxMiniappProperties wx) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.wx = wx;
    }

    /**
     * 使用 login code 换取 openid / unionid（不持久化 session_key）
     */
    public Code2SessionWxResponse code2Session(String code) throws IOException {
        if (!StringUtils.hasText(wx.getAppId()) || !StringUtils.hasText(wx.getAppSecret())) {
            throw new IllegalStateException("未配置 wx.miniapp.app-id / app-secret");
        }
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("code 不能为空");
        }
        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.weixin.qq.com/sns/jscode2session")
                .queryParam("appid", wx.getAppId())
                .queryParam("secret", wx.getAppSecret())
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build(true)
                .toUriString();

        String body = restTemplate.getForObject(url, String.class);
        if (body == null) {
            throw new IOException("微信接口无响应");
        }
        return objectMapper.readValue(body, Code2SessionWxResponse.class);
    }
}
