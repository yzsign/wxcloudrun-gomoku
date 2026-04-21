package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.UserReplayStudySaveRequest;
import com.gomoku.sync.service.SessionJwtService;
import com.gomoku.sync.service.UserReplayStudyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 对局复盘：每用户仅保留一条最新存档，与每日残局接口/表无关。
 */
@RestController
@RequestMapping("/api/me/replay-study")
public class MeReplayStudyController {

    private final UserReplayStudyService userReplayStudyService;
    private final SessionJwtService sessionJwtService;

    public MeReplayStudyController(
            UserReplayStudyService userReplayStudyService, SessionJwtService sessionJwtService) {
        this.userReplayStudyService = userReplayStudyService;
        this.sessionJwtService = sessionJwtService;
    }

    @GetMapping
    public ResponseEntity<?> get(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        return ResponseEntity.ok(userReplayStudyService.get(uid.get()));
    }

    @PutMapping
    public ResponseEntity<?> put(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody UserReplayStudySaveRequest body) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        try {
            userReplayStudyService.save(uid.get(), body);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        userReplayStudyService.clear(uid.get());
        return ResponseEntity.noContent().build();
    }
}
