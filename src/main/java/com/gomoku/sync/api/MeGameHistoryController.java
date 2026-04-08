package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.GameHistoryListResponse;
import com.gomoku.sync.service.GameHistoryService;
import com.gomoku.sync.service.SessionJwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/me")
public class MeGameHistoryController {

    private final GameHistoryService gameHistoryService;
    private final SessionJwtService sessionJwtService;

    public MeGameHistoryController(GameHistoryService gameHistoryService, SessionJwtService sessionJwtService) {
        this.gameHistoryService = gameHistoryService;
        this.sessionJwtService = sessionJwtService;
    }

    /**
     * 已结算联机对局列表（含人机白方），按终局时间倒序。
     * 同时支持 GET 与 POST：部分网关或客户端环境对 GET 返回 405 时可用 POST（query 参数不变）。
     */
    @RequestMapping(value = "/game-history", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> list(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "limit", defaultValue = "30") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        GameHistoryListResponse body = gameHistoryService.listPage(uid.get(), limit, offset);
        return ResponseEntity.ok(body);
    }
}
