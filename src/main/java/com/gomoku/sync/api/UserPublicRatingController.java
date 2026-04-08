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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 按用户 id 查询公开天梯信息（需登录；与房间内 opponent-rating 返回结构一致，无签到字段）。
 */
@RestController
@RequestMapping("/api/users")
public class UserPublicRatingController {

    private final UserMapper userMapper;
    private final SessionJwtService sessionJwtService;

    public UserPublicRatingController(UserMapper userMapper, SessionJwtService sessionJwtService) {
        this.userMapper = userMapper;
        this.sessionJwtService = sessionJwtService;
    }

    @GetMapping("/rating")
    public ResponseEntity<?> userRating(
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
        User u = userMapper.selectById(userId);
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
                av != null && !av.isEmpty() ? av : null,
                u.getGender());
        return ResponseEntity.ok(body);
    }
}
