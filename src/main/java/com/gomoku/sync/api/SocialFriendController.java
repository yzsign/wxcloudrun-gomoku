package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.CreateFriendRequestBody;
import com.gomoku.sync.api.dto.CreateFriendResponse;
import com.gomoku.sync.api.dto.FriendRequestActionResponse;
import com.gomoku.sync.api.dto.FriendStatusResponse;
import com.gomoku.sync.service.SessionJwtService;
import com.gomoku.sync.service.SocialFriendService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 好友申请 REST（friend-request-social-spec §7.1）
 */
@RestController
@RequestMapping("/api/social")
public class SocialFriendController {

    private final SessionJwtService sessionJwtService;
    private final SocialFriendService socialFriendService;

    public SocialFriendController(SessionJwtService sessionJwtService, SocialFriendService socialFriendService) {
        this.sessionJwtService = sessionJwtService;
        this.socialFriendService = socialFriendService;
    }

    @PostMapping("/friend-requests")
    public ResponseEntity<?> createFriendRequest(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) CreateFriendRequestBody body) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        if (body == null || body.getTargetUserId() == null || body.getTargetUserId() <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("BAD_REQUEST", "缺少或非法 targetUserId"));
        }
        try {
            CreateFriendResponse res = socialFriendService.createRequest(uid.get(), body.getTargetUserId());
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", e.getMessage()));
        }
    }

    @PostMapping("/friend-requests/{id}/accept")
    public ResponseEntity<?> accept(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") long id) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        try {
            FriendRequestActionResponse res = socialFriendService.accept(uid.get(), id);
            return ResponseEntity.ok(res);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError("FORBIDDEN", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError("NOT_FOUND", e.getMessage()));
        }
    }

    @PostMapping("/friend-requests/{id}/reject")
    public ResponseEntity<?> reject(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") long id) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        try {
            FriendRequestActionResponse res = socialFriendService.reject(uid.get(), id);
            return ResponseEntity.ok(res);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError("FORBIDDEN", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError("NOT_FOUND", e.getMessage()));
        }
    }

    @GetMapping("/friend-status")
    public ResponseEntity<?> friendStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "userId", required = false) Long userId) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("BAD_REQUEST", "缺少或非法 userId"));
        }
        FriendStatusResponse res = socialFriendService.getFriendStatus(uid.get(), userId);
        return ResponseEntity.ok(res);
    }
}
