package com.gomoku.sync.service;

import com.gomoku.sync.api.dto.PieceSkinRedeemResponse;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.mapper.UserMapper;
import com.gomoku.sync.mapper.UserPieceSkinUnlockMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PieceSkinRedeemService {

    public static final String SKIN_QINGTAO_LIBAI = "qingtao_libai";
    /** 界面棋盘：青瓷 / 水墨，与客户端 themes.THEMES id 一致 */
    public static final String THEME_MINT = "mint";
    public static final String THEME_INK = "ink";

    private static final String SOURCE_POINTS = "ACTIVITY_POINTS";

    private static final Map<String, Integer> SKIN_COST_POINTS;

    static {
        Map<String, Integer> m = new HashMap<>();
        m.put(SKIN_QINGTAO_LIBAI, 200);
        m.put(THEME_MINT, 200);
        m.put(THEME_INK, 200);
        SKIN_COST_POINTS = Collections.unmodifiableMap(m);
    }

    private final UserMapper userMapper;
    private final UserPieceSkinUnlockMapper userPieceSkinUnlockMapper;

    public PieceSkinRedeemService(UserMapper userMapper, UserPieceSkinUnlockMapper userPieceSkinUnlockMapper) {
        this.userMapper = userMapper;
        this.userPieceSkinUnlockMapper = userPieceSkinUnlockMapper;
    }

    public static boolean isRedeemableSkinId(String skinId) {
        return skinId != null && SKIN_COST_POINTS.containsKey(skinId);
    }

    public static int costPointsFor(String skinId) {
        if (skinId == null) {
            return 0;
        }
        Integer c = SKIN_COST_POINTS.get(skinId);
        return c != null ? c : 0;
    }

    /**
     * @return 兑换结果；已拥有时 {@link PieceSkinRedeemResponse#isAlreadyOwned()} 为 true，不扣积分。
     */
    @Transactional
    public PieceSkinRedeemResult redeemWithPoints(long userId, String skinId) {
        if (!isRedeemableSkinId(skinId)) {
            return PieceSkinRedeemResult.invalidSkin();
        }
        int cost = costPointsFor(skinId);
        User u = userMapper.selectByIdForUpdate(userId);
        if (u == null) {
            return PieceSkinRedeemResult.userMissing();
        }
        if (userPieceSkinUnlockMapper.countByUserIdAndSkinId(userId, skinId) > 0) {
            List<String> ids = userPieceSkinUnlockMapper.selectSkinIdsByUserId(userId);
            return PieceSkinRedeemResult.ok(
                    new PieceSkinRedeemResponse(u.getActivityPoints(), ids, true));
        }
        if (u.getActivityPoints() < cost) {
            return PieceSkinRedeemResult.insufficientPoints();
        }
        u.setActivityPoints(u.getActivityPoints() - cost);
        userMapper.updateActivityPoints(u);
        userPieceSkinUnlockMapper.insert(userId, skinId, SOURCE_POINTS, cost);
        List<String> ids = userPieceSkinUnlockMapper.selectSkinIdsByUserId(userId);
        return PieceSkinRedeemResult.ok(
                new PieceSkinRedeemResponse(u.getActivityPoints(), ids, false));
    }

    /** 业务结果 + HTTP 语义 */
    public static final class PieceSkinRedeemResult {
        private final boolean success;
        private final PieceSkinRedeemResponse body;
        private final ErrorKind error;

        private PieceSkinRedeemResult(boolean success, PieceSkinRedeemResponse body, ErrorKind error) {
            this.success = success;
            this.body = body;
            this.error = error;
        }

        static PieceSkinRedeemResult ok(PieceSkinRedeemResponse body) {
            return new PieceSkinRedeemResult(true, body, null);
        }

        static PieceSkinRedeemResult invalidSkin() {
            return new PieceSkinRedeemResult(false, null, ErrorKind.INVALID_SKIN);
        }

        static PieceSkinRedeemResult insufficientPoints() {
            return new PieceSkinRedeemResult(false, null, ErrorKind.INSUFFICIENT_POINTS);
        }

        static PieceSkinRedeemResult userMissing() {
            return new PieceSkinRedeemResult(false, null, ErrorKind.NOT_FOUND);
        }

        public boolean isSuccess() {
            return success;
        }

        public PieceSkinRedeemResponse getBody() {
            return body;
        }

        public ErrorKind getError() {
            return error;
        }

        public enum ErrorKind {
            INVALID_SKIN,
            INSUFFICIENT_POINTS,
            NOT_FOUND
        }
    }
}
