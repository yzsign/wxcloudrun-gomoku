package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.CheckinResponse;
import com.gomoku.sync.service.CheckinService;
import com.gomoku.sync.service.SessionJwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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
}
