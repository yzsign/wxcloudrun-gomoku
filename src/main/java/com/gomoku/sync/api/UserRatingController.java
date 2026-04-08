package com.gomoku.sync.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.UserRatingResponse;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.domain.UserCheckinState;
import com.gomoku.sync.mapper.UserCheckinMapper;
import com.gomoku.sync.mapper.UserMapper;
import com.gomoku.sync.mapper.UserPieceSkinUnlockMapper;
import com.gomoku.sync.service.SessionJwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/me")
public class UserRatingController {

    private final UserMapper userMapper;
    private final UserCheckinMapper userCheckinMapper;
    private final UserPieceSkinUnlockMapper userPieceSkinUnlockMapper;
    private final SessionJwtService sessionJwtService;
    private final ObjectMapper objectMapper;

    public UserRatingController(
            UserMapper userMapper,
            UserCheckinMapper userCheckinMapper,
            UserPieceSkinUnlockMapper userPieceSkinUnlockMapper,
            SessionJwtService sessionJwtService,
            ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.userCheckinMapper = userCheckinMapper;
        this.userPieceSkinUnlockMapper = userPieceSkinUnlockMapper;
        this.sessionJwtService = sessionJwtService;
        this.objectMapper = objectMapper;
    }

    private List<String> parseCheckinHistoryJson(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
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
        UserCheckinState cs = userCheckinMapper.selectByUserId(uid.get());
        List<String> checkinHist =
                cs != null ? parseCheckinHistoryJson(cs.getHistoryJson()) : Collections.emptyList();
        String checkinLastYmd = cs != null ? cs.getLastCheckinYmd() : null;
        int checkinStreak = cs != null ? cs.getStreak() : 0;
        boolean tuanUnlocked = cs != null && cs.isPieceSkinTuanMoeUnlocked();
        List<String> pieceSkinIds = userPieceSkinUnlockMapper.selectSkinIdsByUserId(uid.get());
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
                u.getGender(),
                checkinLastYmd,
                checkinStreak,
                checkinHist,
                tuanUnlocked,
                pieceSkinIds);
        return ResponseEntity.ok(body);
    }
}
