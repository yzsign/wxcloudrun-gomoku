package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.service.AdminTokenService;
import com.gomoku.sync.service.SessionJwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * 当前用户是否为管理端（openid 白名单），供小游戏展示入口。
 */
@RestController
@RequestMapping("/api/me")
public class MeAdminController {

    private final SessionJwtService sessionJwtService;
    private final AdminTokenService adminTokenService;

    public MeAdminController(SessionJwtService sessionJwtService, AdminTokenService adminTokenService) {
        this.sessionJwtService = sessionJwtService;
        this.adminTokenService = adminTokenService;
    }

    @GetMapping("/admin-status")
    public ResponseEntity<?> adminStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        boolean admin = adminTokenService.isUserAdmin(uid.get());
        Map<String, Boolean> body = Collections.singletonMap("admin", admin);
        return ResponseEntity.ok(body);
    }
}
