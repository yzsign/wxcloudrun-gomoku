package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.RandomMatchResponse;
import com.gomoku.sync.service.MatchmakingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final MatchmakingService matchmakingService;

    public MatchController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    /** 进入随机匹配：返回 host（等待对手）或 guest（已配对） */
    @PostMapping("/random")
    public ResponseEntity<RandomMatchResponse> randomMatch() {
        return ResponseEntity.ok(matchmakingService.enter());
    }

    /**
     * 房主取消等待（超时前主动取消或超时后清理）：须校验 blackToken。
     * 若已有白方加入则 409，客户端应转入正常对局。
     */
    @PostMapping("/random/cancel")
    public ResponseEntity<?> cancelRandomMatch(
            @RequestParam("roomId") String roomId,
            @RequestParam("blackToken") String blackToken) {
        MatchmakingService.CancelOutcome o = matchmakingService.cancel(roomId, blackToken);
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
