package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.PieceSkinRedeemRequest;
import com.gomoku.sync.api.dto.PieceSkinSelectRequest;
import com.gomoku.sync.service.PieceSkinRedeemService;
import com.gomoku.sync.service.PieceSkinRedeemService.PieceSkinRedeemResult;
import com.gomoku.sync.service.PieceSkinSelectionService;
import com.gomoku.sync.service.PieceSkinSelectionService.SelectionResult;
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
@RequestMapping("/api/me")
public class MePieceSkinController {

    private final PieceSkinRedeemService pieceSkinRedeemService;
    private final PieceSkinSelectionService pieceSkinSelectionService;
    private final SessionJwtService sessionJwtService;

    public MePieceSkinController(
            PieceSkinRedeemService pieceSkinRedeemService,
            PieceSkinSelectionService pieceSkinSelectionService,
            SessionJwtService sessionJwtService) {
        this.pieceSkinRedeemService = pieceSkinRedeemService;
        this.pieceSkinSelectionService = pieceSkinSelectionService;
        this.sessionJwtService = sessionJwtService;
    }

    @PostMapping("/piece-skin")
    public ResponseEntity<?> selectPieceSkin(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) PieceSkinSelectRequest body) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        if (body == null || body.getPieceSkinId() == null || body.getPieceSkinId().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("BAD_REQUEST", "缺少 pieceSkinId"));
        }
        SelectionResult result = pieceSkinSelectionService.setSelectedPieceSkin(uid.get(), body.getPieceSkinId());
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getBody());
        }
        switch (result.getError()) {
            case BAD_REQUEST:
                return ResponseEntity.badRequest()
                        .body(new ApiError("BAD_REQUEST", "缺少 pieceSkinId"));
            case INVALID_SKIN:
                return ResponseEntity.badRequest()
                        .body(new ApiError("INVALID_SKIN", "不可佩戴该皮肤"));
            case NOT_UNLOCKED:
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiError("NOT_UNLOCKED", "尚未解锁该皮肤"));
            case NOT_FOUND:
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError("NOT_FOUND", "用户不存在"));
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiError("SERVER_ERROR", "保存失败"));
        }
    }

    @PostMapping("/piece-skins/redeem")
    public ResponseEntity<?> redeem(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) PieceSkinRedeemRequest body) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        if (body == null || body.getSkinId() == null || body.getSkinId().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("BAD_REQUEST", "缺少 skinId"));
        }
        String skinId = body.getSkinId().trim();
        PieceSkinRedeemResult result = pieceSkinRedeemService.redeemWithPoints(uid.get(), skinId);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getBody());
        }
        switch (result.getError()) {
            case INVALID_SKIN:
                return ResponseEntity.badRequest()
                        .body(new ApiError("INVALID_SKIN", "不可兑换该皮肤"));
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
}
