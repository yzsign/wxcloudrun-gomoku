package com.gomoku.sync.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.UserRatingResponse;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.domain.UserCheckinState;
import com.gomoku.sync.mapper.UserCheckinMapper;
import com.gomoku.sync.domain.CosmeticCategory;
import com.gomoku.sync.service.ConsumableService;
import com.gomoku.sync.domain.UserEquippedCosmetic;
import com.gomoku.sync.mapper.UserEquippedCosmeticMapper;
import com.gomoku.sync.mapper.UserMapper;
import com.gomoku.sync.mapper.UserPieceSkinUnlockMapper;
import com.gomoku.sync.service.ConsumableService;
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
    private final UserEquippedCosmeticMapper userEquippedCosmeticMapper;
    private final SessionJwtService sessionJwtService;
    private final ObjectMapper objectMapper;
    private final ConsumableService consumableService;

    public UserRatingController(
            UserMapper userMapper,
            UserCheckinMapper userCheckinMapper,
            UserPieceSkinUnlockMapper userPieceSkinUnlockMapper,
            UserEquippedCosmeticMapper userEquippedCosmeticMapper,
            SessionJwtService sessionJwtService,
            ObjectMapper objectMapper,
            ConsumableService consumableService) {
        this.userMapper = userMapper;
        this.userCheckinMapper = userCheckinMapper;
        this.userPieceSkinUnlockMapper = userPieceSkinUnlockMapper;
        this.userEquippedCosmeticMapper = userEquippedCosmeticMapper;
        this.sessionJwtService = sessionJwtService;
        this.objectMapper = objectMapper;
        this.consumableService = consumableService;
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
        String pieceSlot = null;
        String themeSlot = null;
        String boardSkillSlot = null;
        String boardSkillLoveSlot = null;
        for (UserEquippedCosmetic row : userEquippedCosmeticMapper.selectByUserId(uid.get())) {
            if (row == null || row.getCategory() == null) {
                continue;
            }
            if (CosmeticCategory.PIECE_SKIN.equals(row.getCategory())) {
                pieceSlot = row.getItemId();
            } else if (CosmeticCategory.THEME.equals(row.getCategory())) {
                themeSlot = row.getItemId();
            } else if (CosmeticCategory.BOARD_SKILL.equals(row.getCategory())) {
                boardSkillSlot = row.getItemId();
            } else if (CosmeticCategory.BOARD_SKILL_LOVE.equals(row.getCategory())) {
                boardSkillLoveSlot = row.getItemId();
            }
        }
        String pieceSkinOut = pieceSlot != null && !pieceSlot.isEmpty() ? pieceSlot : u.getPieceSkinId();
        boolean daggerEquipped =
                boardSkillSlot != null
                        && ConsumableService.KIND_DAGGER.equalsIgnoreCase(boardSkillSlot.trim());
        boolean loveEquipped =
                boardSkillLoveSlot != null
                        && ConsumableService.KIND_LOVE.equalsIgnoreCase(boardSkillLoveSlot.trim());
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
                pieceSkinIds,
                pieceSkinOut,
                themeSlot,
                daggerEquipped,
                consumableService.getDaggerCount(uid.get()),
                loveEquipped,
                consumableService.getLoveCount(uid.get()));
        return ResponseEntity.ok(body);
    }
}
