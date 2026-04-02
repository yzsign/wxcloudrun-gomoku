package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.SilentLoginRequest;
import com.gomoku.sync.api.dto.SilentLoginResponse;
import com.gomoku.sync.service.SilentAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SilentAuthService silentAuthService;

    public AuthController(SilentAuthService silentAuthService) {
        this.silentAuthService = silentAuthService;
    }

    /**
     * 静默登录：body JSON { "code": "wx.login 返回的 code", "nickname": "可选", "avatarUrl": "可选" }
     */
    @PostMapping("/silent-login")
    public ResponseEntity<?> silentLogin(@RequestBody SilentLoginRequest body) {
        if (body == null || body.getCode() == null || body.getCode().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("INVALID_CODE", "缺少 code"));
        }
        try {
            SilentLoginResponse ok = silentAuthService.silentLogin(body);
            return ResponseEntity.ok(ok);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiError("WX_NOT_CONFIGURED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("WX_LOGIN_FAILED", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ApiError("WX_HTTP_ERROR", e.getMessage()));
        }
    }
}
