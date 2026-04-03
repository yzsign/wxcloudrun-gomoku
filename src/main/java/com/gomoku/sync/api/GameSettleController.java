package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.SettleGameRequest;
import com.gomoku.sync.api.dto.SettleGameResponse;
import com.gomoku.sync.service.RatingSettlementService;
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
@RequestMapping("/api/games")
public class GameSettleController {

    private final RatingSettlementService ratingSettlementService;
    private final SessionJwtService sessionJwtService;

    public GameSettleController(
            RatingSettlementService ratingSettlementService,
            SessionJwtService sessionJwtService) {
        this.ratingSettlementService = ratingSettlementService;
        this.sessionJwtService = sessionJwtService;
    }

    /**
     * 对局结算：须 JWT；请求方须为该局黑方或白方。同一 roomId 仅可成功一次。
     */
    @PostMapping("/settle")
    public ResponseEntity<?> settle(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SettleGameRequest body) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        try {
            SettleGameResponse ok = ratingSettlementService.settle(uid.get(), body);
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("BAD_REQUEST", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("CONFLICT", e.getMessage()));
        }
    }
}
