package com.gomoku.sync.service;

import com.gomoku.sync.api.dto.PieceSkinSelectResponse;
import com.gomoku.sync.domain.CosmeticCategory;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.domain.UserCheckinState;
import com.gomoku.sync.mapper.UserCheckinMapper;
import com.gomoku.sync.mapper.UserEquippedCosmeticMapper;
import com.gomoku.sync.mapper.UserMapper;
import com.gomoku.sync.mapper.UserPieceSkinUnlockMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 佩戴棋子皮肤：与客户端目录可选项一致，须已解锁。
 */
@Service
public class PieceSkinSelectionService {

    public static final String SKIN_BASIC = "basic";
    public static final String SKIN_TUAN_MOE = "tuan_moe";

    private static final Set<String> SELECTABLE_SKIN_IDS;

    static {
        Set<String> s = new HashSet<>();
        s.add(SKIN_BASIC);
        s.add(SKIN_TUAN_MOE);
        s.add(PieceSkinRedeemService.SKIN_QINGTAO_LIBAI);
        SELECTABLE_SKIN_IDS = Collections.unmodifiableSet(s);
    }

    private final UserMapper userMapper;
    private final UserCheckinMapper userCheckinMapper;
    private final UserPieceSkinUnlockMapper userPieceSkinUnlockMapper;
    private final UserEquippedCosmeticMapper userEquippedCosmeticMapper;

    public PieceSkinSelectionService(
            UserMapper userMapper,
            UserCheckinMapper userCheckinMapper,
            UserPieceSkinUnlockMapper userPieceSkinUnlockMapper,
            UserEquippedCosmeticMapper userEquippedCosmeticMapper) {
        this.userMapper = userMapper;
        this.userCheckinMapper = userCheckinMapper;
        this.userPieceSkinUnlockMapper = userPieceSkinUnlockMapper;
        this.userEquippedCosmeticMapper = userEquippedCosmeticMapper;
    }

    public static boolean isSelectableSkinId(String skinId) {
        return skinId != null && SELECTABLE_SKIN_IDS.contains(skinId);
    }

    /**
     * 将数据库或请求中的原始值规范为可展示/可存档的 id（非法或空则 basic）。
     */
    public static String sanitizeStoredPieceSkinId(String raw) {
        if (raw == null || raw.isBlank()) {
            return SKIN_BASIC;
        }
        String t = raw.trim();
        return isSelectableSkinId(t) ? t : SKIN_BASIC;
    }

    @Transactional
    public SelectionResult setSelectedPieceSkin(long userId, String rawSkinId) {
        if (rawSkinId == null || rawSkinId.trim().isEmpty()) {
            return SelectionResult.badRequest();
        }
        String skinId = rawSkinId.trim();
        if (!isSelectableSkinId(skinId)) {
            return SelectionResult.invalidSkin();
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            return SelectionResult.notFound();
        }
        if (!canWear(userId, skinId)) {
            return SelectionResult.notUnlocked();
        }
        userEquippedCosmeticMapper.upsert(userId, CosmeticCategory.PIECE_SKIN, skinId);
        userMapper.updatePieceSkinId(userId, skinId);
        return SelectionResult.ok(new PieceSkinSelectResponse(skinId));
    }

    private boolean canWear(long userId, String skinId) {
        if (SKIN_BASIC.equals(skinId)) {
            return true;
        }
        if (SKIN_TUAN_MOE.equals(skinId)) {
            UserCheckinState cs = userCheckinMapper.selectByUserId(userId);
            return cs != null && cs.isPieceSkinTuanMoeUnlocked();
        }
        if (PieceSkinRedeemService.SKIN_QINGTAO_LIBAI.equals(skinId)) {
            return userPieceSkinUnlockMapper.countByUserIdAndSkinId(userId, skinId) > 0;
        }
        return false;
    }

    public enum SelectionError {
        BAD_REQUEST,
        INVALID_SKIN,
        NOT_UNLOCKED,
        NOT_FOUND
    }

    public static final class SelectionResult {
        private final boolean success;
        private final PieceSkinSelectResponse body;
        private final SelectionError error;

        private SelectionResult(boolean success, PieceSkinSelectResponse body, SelectionError error) {
            this.success = success;
            this.body = body;
            this.error = error;
        }

        static SelectionResult ok(PieceSkinSelectResponse body) {
            return new SelectionResult(true, body, null);
        }

        static SelectionResult badRequest() {
            return new SelectionResult(false, null, SelectionError.BAD_REQUEST);
        }

        static SelectionResult invalidSkin() {
            return new SelectionResult(false, null, SelectionError.INVALID_SKIN);
        }

        static SelectionResult notUnlocked() {
            return new SelectionResult(false, null, SelectionError.NOT_UNLOCKED);
        }

        static SelectionResult notFound() {
            return new SelectionResult(false, null, SelectionError.NOT_FOUND);
        }

        public boolean isSuccess() {
            return success;
        }

        public PieceSkinSelectResponse getBody() {
            return body;
        }

        public SelectionError getError() {
            return error;
        }
    }
}
