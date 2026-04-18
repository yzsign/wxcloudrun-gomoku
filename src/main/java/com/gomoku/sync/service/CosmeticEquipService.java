package com.gomoku.sync.service;

import com.gomoku.sync.api.dto.EquipResponse;
import com.gomoku.sync.domain.CosmeticCategory;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.mapper.UserEquippedCosmeticMapper;
import com.gomoku.sync.mapper.UserMapper;
import com.gomoku.sync.mapper.UserPieceSkinUnlockMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 杂货铺装备：每种 {@link CosmeticCategory} 至多一件；新增种类时在域层增加常量并在此分支校验与写入。
 */
@Service
public class CosmeticEquipService {

    private static final String THEME_CLASSIC = "classic";
    private static final String BOARD_SKILL_ITEM_DAGGER = "dagger";
    private static final String BOARD_SKILL_ITEM_LOVE = "love";
    private static final String BOARD_SKILL_CLEAR = "off";

    private final UserEquippedCosmeticMapper userEquippedCosmeticMapper;
    private final UserMapper userMapper;
    private final UserPieceSkinUnlockMapper userPieceSkinUnlockMapper;
    private final PieceSkinSelectionService pieceSkinSelectionService;

    public CosmeticEquipService(
            UserEquippedCosmeticMapper userEquippedCosmeticMapper,
            UserMapper userMapper,
            UserPieceSkinUnlockMapper userPieceSkinUnlockMapper,
            PieceSkinSelectionService pieceSkinSelectionService) {
        this.userEquippedCosmeticMapper = userEquippedCosmeticMapper;
        this.userMapper = userMapper;
        this.userPieceSkinUnlockMapper = userPieceSkinUnlockMapper;
        this.pieceSkinSelectionService = pieceSkinSelectionService;
    }

    @Transactional
    public EquipResult equip(long userId, String rawCategory, String rawItemId) {
        if (rawCategory == null || rawItemId == null) {
            return EquipResult.badRequest();
        }
        String cat = rawCategory.trim();
        String itemId = rawItemId.trim();
        if (cat.isEmpty() || itemId.isEmpty()) {
            return EquipResult.badRequest();
        }
        if (CosmeticCategory.PIECE_SKIN.equalsIgnoreCase(cat)) {
            PieceSkinSelectionService.SelectionResult sr =
                    pieceSkinSelectionService.setSelectedPieceSkin(userId, itemId);
            if (!sr.isSuccess()) {
                return EquipResult.fromPieceSkinSelection(sr);
            }
            return EquipResult.ok(
                    new EquipResponse(CosmeticCategory.PIECE_SKIN, sr.getBody().getPieceSkinId()));
        }
        if (CosmeticCategory.THEME.equalsIgnoreCase(cat)) {
            return equipTheme(userId, itemId);
        }
        if (CosmeticCategory.BOARD_SKILL.equalsIgnoreCase(cat)) {
            return equipBoardSkill(userId, itemId);
        }
        if (CosmeticCategory.BOARD_SKILL_LOVE.equalsIgnoreCase(cat)) {
            return equipBoardSkillLove(userId, itemId);
        }
        return EquipResult.unknownCategory();
    }

    private EquipResult equipBoardSkill(long userId, String rawItemId) {
        if (rawItemId == null) {
            return EquipResult.badRequest();
        }
        String itemId = rawItemId.trim();
        if (itemId.isEmpty()) {
            return EquipResult.badRequest();
        }
        if (BOARD_SKILL_CLEAR.equalsIgnoreCase(itemId)) {
            userEquippedCosmeticMapper.deleteByUserIdAndCategory(userId, CosmeticCategory.BOARD_SKILL);
            return EquipResult.ok(new EquipResponse(CosmeticCategory.BOARD_SKILL, null));
        }
        if (!BOARD_SKILL_ITEM_DAGGER.equalsIgnoreCase(itemId)) {
            return EquipResult.invalidItem();
        }
        userEquippedCosmeticMapper.upsert(userId, CosmeticCategory.BOARD_SKILL, BOARD_SKILL_ITEM_DAGGER);
        return EquipResult.ok(new EquipResponse(CosmeticCategory.BOARD_SKILL, BOARD_SKILL_ITEM_DAGGER));
    }

    private EquipResult equipBoardSkillLove(long userId, String rawItemId) {
        if (rawItemId == null) {
            return EquipResult.badRequest();
        }
        String itemId = rawItemId.trim();
        if (itemId.isEmpty()) {
            return EquipResult.badRequest();
        }
        if (BOARD_SKILL_CLEAR.equalsIgnoreCase(itemId)) {
            userEquippedCosmeticMapper.deleteByUserIdAndCategory(userId, CosmeticCategory.BOARD_SKILL_LOVE);
            return EquipResult.ok(new EquipResponse(CosmeticCategory.BOARD_SKILL_LOVE, null));
        }
        if (!BOARD_SKILL_ITEM_LOVE.equalsIgnoreCase(itemId)) {
            return EquipResult.invalidItem();
        }
        userEquippedCosmeticMapper.upsert(userId, CosmeticCategory.BOARD_SKILL_LOVE, BOARD_SKILL_ITEM_LOVE);
        return EquipResult.ok(new EquipResponse(CosmeticCategory.BOARD_SKILL_LOVE, BOARD_SKILL_ITEM_LOVE));
    }

    private EquipResult equipTheme(long userId, String themeId) {
        if (!isSelectableThemeId(themeId)) {
            return EquipResult.invalidItem();
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            return EquipResult.notFound();
        }
        if (!canWearTheme(userId, themeId)) {
            return EquipResult.notUnlocked();
        }
        userEquippedCosmeticMapper.upsert(userId, CosmeticCategory.THEME, themeId);
        return EquipResult.ok(new EquipResponse(CosmeticCategory.THEME, themeId));
    }

    private static boolean isSelectableThemeId(String themeId) {
        return THEME_CLASSIC.equals(themeId)
                || PieceSkinRedeemService.THEME_MINT.equals(themeId)
                || PieceSkinRedeemService.THEME_INK.equals(themeId);
    }

    private boolean canWearTheme(long userId, String themeId) {
        if (THEME_CLASSIC.equals(themeId)) {
            return true;
        }
        if (PieceSkinRedeemService.THEME_MINT.equals(themeId)
                || PieceSkinRedeemService.THEME_INK.equals(themeId)) {
            return userPieceSkinUnlockMapper.countByUserIdAndSkinId(userId, themeId) > 0;
        }
        return false;
    }

    public enum EquipError {
        BAD_REQUEST,
        UNKNOWN_CATEGORY,
        INVALID_ITEM,
        NOT_UNLOCKED,
        NOT_FOUND,
        PIECE_SKIN_BAD_REQUEST,
        PIECE_SKIN_INVALID,
        PIECE_SKIN_NOT_UNLOCKED
    }

    public static final class EquipResult {
        private final boolean success;
        private final EquipResponse body;
        private final EquipError error;

        private EquipResult(boolean success, EquipResponse body, EquipError error) {
            this.success = success;
            this.body = body;
            this.error = error;
        }

        static EquipResult ok(EquipResponse body) {
            return new EquipResult(true, body, null);
        }

        static EquipResult badRequest() {
            return new EquipResult(false, null, EquipError.BAD_REQUEST);
        }

        static EquipResult unknownCategory() {
            return new EquipResult(false, null, EquipError.UNKNOWN_CATEGORY);
        }

        static EquipResult invalidItem() {
            return new EquipResult(false, null, EquipError.INVALID_ITEM);
        }

        static EquipResult notUnlocked() {
            return new EquipResult(false, null, EquipError.NOT_UNLOCKED);
        }

        static EquipResult notFound() {
            return new EquipResult(false, null, EquipError.NOT_FOUND);
        }

        static EquipResult fromPieceSkinSelection(PieceSkinSelectionService.SelectionResult sr) {
            switch (sr.getError()) {
                case BAD_REQUEST:
                    return new EquipResult(false, null, EquipError.PIECE_SKIN_BAD_REQUEST);
                case INVALID_SKIN:
                    return new EquipResult(false, null, EquipError.PIECE_SKIN_INVALID);
                case NOT_UNLOCKED:
                    return new EquipResult(false, null, EquipError.PIECE_SKIN_NOT_UNLOCKED);
                case NOT_FOUND:
                    return notFound();
                default:
                    return new EquipResult(false, null, EquipError.BAD_REQUEST);
            }
        }

        public boolean isSuccess() {
            return success;
        }

        public EquipResponse getBody() {
            return body;
        }

        public EquipError getError() {
            return error;
        }
    }
}
