package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.CheckinMakeupRequest;
import com.gomoku.sync.api.dto.CheckinResponse;
import com.gomoku.sync.service.CheckinService;
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
public class MeCheckinController {

    private final CheckinService checkinService;
    private final SessionJwtService sessionJwtService;

    public MeCheckinController(CheckinService checkinService, SessionJwtService sessionJwtService) {
        this.checkinService = checkinService;
        this.sessionJwtService = sessionJwtService;
    }

    @PostMapping("/checkin")
    public ResponseEntity<?> checkin(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        CheckinResponse body = checkinService.checkin(uid.get());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/checkin/makeup")
    public ResponseEntity<?> checkinMakeup(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) CheckinMakeupRequest body) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        String ymd = body != null ? body.getYmd() : null;
        CheckinService.MakeupOutcome outcome = checkinService.checkinMakeup(uid.get(), ymd);
        if (!outcome.isOk()) {
            switch (outcome.getError()) {
                case INSUFFICIENT_POINTS:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ApiError("INSUFFICIENT_POINTS", "积分不足"));
                case ALREADY_SIGNED:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ApiError("ALREADY_SIGNED", "该日已签到"));
                case CANNOT_MAKEUP_TODAY:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ApiError("CANNOT_MAKEUP_TODAY", "请使用今日签到"));
                case FUTURE_DATE:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ApiError("FUTURE_DATE", "不能补签未来日期"));
                case OUT_OF_RANGE:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ApiError("OUT_OF_RANGE", "仅支持最近12个月内补签"));
                case INVALID_DATE:
                default:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ApiError("INVALID_DATE", "日期无效"));
            }
        }
        return ResponseEntity.ok(outcome.getResponse());
    }
}
