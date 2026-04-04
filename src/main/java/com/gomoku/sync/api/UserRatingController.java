package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.UserRatingResponse;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.mapper.UserMapper;
import com.gomoku.sync.service.SessionJwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/me")
public class UserRatingController {

    private final UserMapper userMapper;
    private final SessionJwtService sessionJwtService;

    public UserRatingController(UserMapper userMapper, SessionJwtService sessionJwtService) {
        this.userMapper = userMapper;
        this.sessionJwtService = sessionJwtService;
    }

    @GetMapping("/rating")
    public ResponseEntity<?> rating(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        User u = userMapper.selectById(uid.get());
        if (u == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError("NOT_FOUND", "用户不存在"));
        }
        String nick = u.getNickname();
        String av = u.getAvatarUrl();
        UserRatingResponse body = new UserRatingResponse(
                u.getId().longValue(),
                u.getEloScore(),
                u.getActivityPoints(),
                u.getConsecutiveWins(),
                u.getConsecutiveLosses(),
                u.getTotalGames(),
                u.getWinCount(),
                u.getDrawCount(),
                u.getRunawayCount(),
                u.isLowTrust(),
                u.getPlacementFairGames(),
                u.getNewbieMatchGames(),
                nick != null && !nick.isEmpty() ? nick : null,
                av != null && !av.isEmpty() ? av : null);
        return ResponseEntity.ok(body);
    }
}
