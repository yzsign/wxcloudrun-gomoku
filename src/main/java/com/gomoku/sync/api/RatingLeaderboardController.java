package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.LeaderboardEntryDto;
import com.gomoku.sync.api.dto.LeaderboardListResponse;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 天梯实时榜（rule.md §8.2）；需登录；人机与真人同一排序规则，客户端不做区分。
 */
@RestController
@RequestMapping("/api/rating")
public class RatingLeaderboardController {

    private static final int MAX_LIMIT = 100;

    private final UserMapper userMapper;
    private final SessionJwtService sessionJwtService;

    public RatingLeaderboardController(UserMapper userMapper, SessionJwtService sessionJwtService) {
        this.userMapper = userMapper;
        this.sessionJwtService = sessionJwtService;
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<?> leaderboard(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "limit", defaultValue = "30") int limit) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        int lim = limit;
        if (lim < 1) {
            lim = 1;
        }
        if (lim > MAX_LIMIT) {
            lim = MAX_LIMIT;
        }
        List<User> rows = userMapper.selectLeaderboardUsers(lim);
        List<LeaderboardEntryDto> items = new ArrayList<>(rows.size());
        int rank = 1;
        for (User u : rows) {
            if (u == null || u.getId() == null) {
                continue;
            }
            String nick = u.getNickname();
            String av = u.getAvatarUrl();
            items.add(
                    new LeaderboardEntryDto(
                            rank++,
                            u.getId().longValue(),
                            nick != null && !nick.isEmpty() ? nick : "棋手",
                            av != null && !av.isEmpty() ? av : null,
                            u.getEloScore(),
                            u.getTitleName(),
                            u.getTotalGames(),
                            u.getWinCount()));
        }
        return ResponseEntity.ok(new LeaderboardListResponse(items));
    }
}
