package com.gomoku.sync.service;

import com.gomoku.sync.api.dto.PieceSkinSelectResponse;
import com.gomoku.sync.domain.CosmeticCategory;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.domain.UserCheckinState;
import com.gomoku.sync.domain.UserEquippedCosmetic;
import com.gomoku.sync.mapper.ShopMapper;
import com.gomoku.sync.mapper.UserCheckinMapper;
import com.gomoku.sync.mapper.UserEquippedCosmeticMapper;
import com.gomoku.sync.mapper.UserMapper;
import com.gomoku.sync.mapper.UserPieceSkinUnlockMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 佩戴棋子皮肤：须已解锁；可佩戴 id = basic / tuan_moe / 或 shop 上架的 piece_skin。
 */
@Service
public class PieceSkinSelectionService {

    public static final String SKIN_BASIC = "basic";
    public static final String SKIN_TUAN_MOE = "tuan_moe";

    private final UserMapper userMapper;
    private final UserCheckinMapper userCheckinMapper;
    private final UserPieceSkinUnlockMapper userPieceSkinUnlockMapper;
    private final UserEquippedCosmeticMapper userEquippedCosmeticMapper;
    private final ShopMapper shopMapper;

    public PieceSkinSelectionService(
            UserMapper userMapper,
            UserCheckinMapper userCheckinMapper,
            UserPieceSkinUnlockMapper userPieceSkinUnlockMapper,
            UserEquippedCosmeticMapper userEquippedCosmeticMapper,
            ShopMapper shopMapper) {
        this.userMapper = userMapper;
        this.userCheckinMapper = userCheckinMapper;
        this.userPieceSkinUnlockMapper = userPieceSkinUnlockMapper;
        this.userEquippedCosmeticMapper = userEquippedCosmeticMapper;
        this.shopMapper = shopMapper;
    }

    public boolean isSelectableSkinId(String skinId) {
        if (skinId == null || skinId.isEmpty()) {
            return false;
        }
        if (SKIN_BASIC.equals(skinId) || SKIN_TUAN_MOE.equals(skinId)) {
            return true;
        }
        return shopMapper.countEnabledPieceSkinByCode(skinId) > 0;
    }

    /**
     * 将数据库或请求中的原始值规范为可展示/可存档的 id（非法或空则 basic）。
     */
    public static String sanitizeStoredPieceSkinId(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return SKIN_BASIC;
        }
        String t = raw.trim();
        if (SKIN_BASIC.equals(t) || SKIN_TUAN_MOE.equals(t)) {
            return t;
        }
        if (t.length() <= 32 && t.matches("[a-z][a-z0-9_]*")) {
            return t;
        }
        return SKIN_BASIC;
    }

    /**
     * 当前佩戴棋子皮肤：与 {@code GET /api/me/rating} 的 pieceSkinId 一致（装备槽 PIECE_SKIN 优先于 users.piece_skin_id）。
     * 用于 WS STATE 广播、对局归档等，避免两列不同步时对手端仍看到 basic。
     */
    public String resolveEquippedPieceSkinForBroadcast(long userId) {
        String pieceSlot = null;
        for (UserEquippedCosmetic row : userEquippedCosmeticMapper.selectByUserId(userId)) {
            if (row == null || row.getCategory() == null) {
                continue;
            }
            if (CosmeticCategory.PIECE_SKIN.equals(row.getCategory())) {
                pieceSlot = row.getItemId();
                break;
            }
        }
        if (pieceSlot != null && !pieceSlot.isEmpty()) {
            return sanitizeStoredPieceSkinId(pieceSlot);
        }
        return sanitizeStoredPieceSkinId(userMapper.selectPieceSkinIdByUserId(userId));
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
        return userPieceSkinUnlockMapper.countByUserIdAndSkinId(userId, skinId) > 0;
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
