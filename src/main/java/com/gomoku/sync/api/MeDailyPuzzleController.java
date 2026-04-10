package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.DailyPuzzleHintResponse;
import com.gomoku.sync.api.dto.DailyPuzzleSubmitRequest;
import com.gomoku.sync.api.dto.DailyPuzzleSubmitResponse;
import com.gomoku.sync.api.dto.DailyPuzzleTodayResponse;
import com.gomoku.sync.service.DailyPuzzleService;
import com.gomoku.sync.service.SessionJwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/me")
public class MeDailyPuzzleController {

    private final DailyPuzzleService dailyPuzzleService;
    private final SessionJwtService sessionJwtService;

    public MeDailyPuzzleController(DailyPuzzleService dailyPuzzleService, SessionJwtService sessionJwtService) {
        this.dailyPuzzleService = dailyPuzzleService;
        this.sessionJwtService = sessionJwtService;
    }

    @GetMapping("/daily-puzzle/today")
    public ResponseEntity<?> today(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        DailyPuzzleTodayResponse body = dailyPuzzleService.getToday(uid.get());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/daily-puzzle/submit")
    public ResponseEntity<?> submit(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody DailyPuzzleSubmitRequest body) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        try {
            DailyPuzzleSubmitResponse res = dailyPuzzleService.submit(uid.get(), body);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiError("UNAVAILABLE", e.getMessage()));
        }
    }

    @PostMapping("/daily-puzzle/hint")
    public ResponseEntity<?> hint(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        try {
            DailyPuzzleHintResponse res = dailyPuzzleService.useHint(uid.get());
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiError("UNAVAILABLE", e.getMessage()));
        }
    }
}
