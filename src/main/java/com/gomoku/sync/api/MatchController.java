package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.RandomMatchResponse;
import com.gomoku.sync.service.MatchmakingService;
import com.gomoku.sync.service.SessionJwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final MatchmakingService matchmakingService;
    private final SessionJwtService sessionJwtService;

    public MatchController(MatchmakingService matchmakingService, SessionJwtService sessionJwtService) {
        this.matchmakingService = matchmakingService;
        this.sessionJwtService = sessionJwtService;
    }

    /** 进入随机匹配：返回 host（等待对手）或 guest（已配对） */
    @PostMapping("/random")
    public ResponseEntity<?> randomMatch(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录（需 Authorization: Bearer sessionToken）"));
        }
        return ResponseEntity.ok(matchmakingService.enter(uid.get()));
    }

    /**
     * 房主取消等待（超时前主动取消或超时后清理）：须校验 blackToken。
     * 若已有白方加入则 409，客户端应转入正常对局。
     */
    @PostMapping("/random/cancel")
    public ResponseEntity<?> cancelRandomMatch(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("roomId") String roomId,
            @RequestParam("blackToken") String blackToken) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        MatchmakingService.CancelOutcome o = matchmakingService.cancel(roomId, blackToken, uid.get());
        if (o == MatchmakingService.CancelOutcome.OK) {
            return ResponseEntity.ok().build();
        }
        if (o == MatchmakingService.CancelOutcome.HAS_GUEST) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("ROOM_HAS_GUEST", "已有对手加入"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("ROOM_NOT_FOUND", "无法取消"));
    }
}
