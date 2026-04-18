package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.ConsumableKindRequest;
import com.gomoku.sync.service.ConsumableService;
import com.gomoku.sync.service.ConsumableService.ConsumableResult;
import com.gomoku.sync.service.SessionJwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/me/consumables")
public class MeConsumableController {

    private final ConsumableService consumableService;
    private final SessionJwtService sessionJwtService;

    public MeConsumableController(ConsumableService consumableService, SessionJwtService sessionJwtService) {
        this.consumableService = consumableService;
        this.sessionJwtService = sessionJwtService;
    }

    @PostMapping("/redeem")
    public ResponseEntity<?> redeem(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) ConsumableKindRequest body) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        String kind = body != null ? body.getKind() : null;
        ConsumableResult result = consumableService.redeemWithPoints(uid.get(), kind);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getBody());
        }
        switch (result.getError()) {
            case INVALID_KIND:
                return ResponseEntity.badRequest()
                        .body(new ApiError("INVALID_KIND", "不支持的消耗品"));
            case INSUFFICIENT_POINTS:
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiError("INSUFFICIENT_POINTS", "积分不足"));
            case NOT_FOUND:
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError("NOT_FOUND", "用户不存在"));
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiError("SERVER_ERROR", "兑换失败"));
        }
    }

    @PostMapping("/use")
    public ResponseEntity<?> use(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) ConsumableKindRequest body) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        String kind = body != null ? body.getKind() : null;
        ConsumableResult result = consumableService.useOne(uid.get(), kind);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getBody());
        }
        switch (result.getError()) {
            case INVALID_KIND:
                return ResponseEntity.badRequest()
                        .body(new ApiError("INVALID_KIND", "不支持的消耗品"));
            case NONE_LEFT:
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiError("NONE_LEFT", "短剑数量不足"));
            case NOT_FOUND:
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError("NOT_FOUND", "用户不存在"));
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiError("SERVER_ERROR", "操作失败"));
        }
    }
}
