package com.gomoku.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gomoku.sync.config.WxMiniappProperties;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.mapper.UserMapper;
import com.gomoku.sync.wx.MsgSecCheckWxResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

/**
 * 微信「文本内容安全」{@code msg_sec_check} 2.0，用于对局聊天兜底（需在服务端调用）。
 *
 * @see <a href="https://developers.weixin.qq.com/miniprogram/dev/server/API/sec-center/sec-check/api_msgseccheck.html">msgSecCheck</a>
 */
@Service
public class WeChatMsgSecCheckService {

    private static final Logger log = LoggerFactory.getLogger(WeChatMsgSecCheckService.class);

    /** 内容违规（旧版等场景仍可能返回） */
    private static final int ERRCODE_CONTENT_RISKY = 87014;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WxMiniappProperties wx;
    private final WxAccessTokenService wxAccessTokenService;
    private final UserMapper userMapper;

    public WeChatMsgSecCheckService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            WxMiniappProperties wx,
            WxAccessTokenService wxAccessTokenService,
            UserMapper userMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.wx = wx;
        this.wxAccessTokenService = wxAccessTokenService;
        this.userMapper = userMapper;
    }

    /**
     * @param userId 发送者用户 id（查 openid）
     * @param content 已本地脱敏后的待广播文本
     * @return 若不应发送则返回错误文案；通过或跳过校验则 empty
     */
    public Optional<String> rejectIfWeChatRisky(long userId, String content) {
        if (!wx.isMsgSecCheckEnabled()) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(wx.getAppSecret())) {
            return Optional.empty();
        }
        User user = userMapper.selectById(userId);
        if (user == null || user.isBot() || !StringUtils.hasText(user.getOpenid())) {
            return Optional.empty();
        }
        boolean failOpen = wx.isMsgSecCheckFailOpenOnError();
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String token = wxAccessTokenService.getAccessToken();
                String url =
                        UriComponentsBuilder.fromHttpUrl("https://api.weixin.qq.com/wxa/msg_sec_check")
                                .queryParam("access_token", token)
                                .build(true)
                                .toUriString();
                ObjectNode body = objectMapper.createObjectNode();
                body.put("version", 2);
                body.put("openid", user.getOpenid());
                body.put("scene", wx.getMsgSecCheckScene());
                body.put("content", content);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                String raw =
                        restTemplate.postForObject(
                                url, new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class);
                if (raw == null) {
                    return maybeFailOpen(failOpen, "微信内容安全无响应");
                }
                MsgSecCheckWxResponse resp = objectMapper.readValue(raw, MsgSecCheckWxResponse.class);
                int err = resp.getErrcode() != null ? resp.getErrcode() : 0;
                if (err == 40001 || err == 42001) {
                    wxAccessTokenService.invalidate();
                    continue;
                }
                if (err == ERRCODE_CONTENT_RISKY) {
                    return Optional.of("内容含有违规信息，无法发送");
                }
                if (err != 0) {
                    log.warn("msg_sec_check errcode={} errmsg={}", err, resp.getErrmsg());
                    return maybeFailOpen(failOpen, "微信内容安全接口错误: " + err);
                }
                MsgSecCheckWxResponse.Result res = resp.getResult();
                if (res != null && "risky".equalsIgnoreCase(res.getSuggest())) {
                    return Optional.of("内容含有违规信息，无法发送");
                }
                return Optional.empty();
            } catch (IOException | RuntimeException e) {
                log.warn("msg_sec_check failed userId={}: {}", userId, e.getMessage());
                return maybeFailOpen(failOpen, e.getMessage());
            }
        }
        return maybeFailOpen(failOpen, "access_token 重试后仍失败");
    }

    private Optional<String> maybeFailOpen(boolean failOpen, String detail) {
        if (failOpen) {
            log.debug("msg_sec_check skipped due to error: {}", detail);
            return Optional.empty();
        }
        return Optional.of("内容校验失败，请稍后重试");
    }
}
