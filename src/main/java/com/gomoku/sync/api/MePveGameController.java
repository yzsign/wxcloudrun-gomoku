package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.RecordPveGameRequest;
import com.gomoku.sync.api.dto.RecordPveGameResponse;
import com.gomoku.sync.service.PveGameService;
import com.gomoku.sync.service.SessionJwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/me")
public class MePveGameController {

    private final PveGameService pveGameService;
    private final SessionJwtService sessionJwtService;

    public MePveGameController(PveGameService pveGameService, SessionJwtService sessionJwtService) {
        this.pveGameService = pveGameService;
        this.sessionJwtService = sessionJwtService;
    }

    /**
     * 人机对局终局归档到 games（不计分）；需登录。
     */
    @RequestMapping(value = "/pve-game", method = RequestMethod.POST)
    public ResponseEntity<?> recordPve(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody RecordPveGameRequest body) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        if (body == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("BAD_REQUEST", "请求体为空"));
        }
        try {
            long gameId = pveGameService.recordPveGame(uid.get(), body);
            return ResponseEntity.ok(new RecordPveGameResponse(gameId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("BAD_REQUEST", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiError("UNAVAILABLE", e.getMessage()));
        }
    }
}
