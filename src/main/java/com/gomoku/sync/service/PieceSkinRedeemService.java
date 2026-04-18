package com.gomoku.sync.service;

import com.gomoku.sync.api.dto.PieceSkinRedeemResponse;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.mapper.UserMapper;
import com.gomoku.sync.mapper.UserPieceSkinUnlockMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PieceSkinRedeemService {

    public static final String SKIN_QINGTAO_LIBAI = "qingtao_libai";
    /** 界面棋盘：青瓷 / 水墨，与客户端 themes.THEMES id 一致 */
    public static final String THEME_MINT = "mint";
    public static final String THEME_INK = "ink";

    private static final String SOURCE_POINTS = "ACTIVITY_POINTS";

    private final UserMapper userMapper;
    private final UserPieceSkinUnlockMapper userPieceSkinUnlockMapper;
    private final ShopPricingService shopPricingService;

    public PieceSkinRedeemService(
            UserMapper userMapper,
            UserPieceSkinUnlockMapper userPieceSkinUnlockMapper,
            ShopPricingService shopPricingService) {
        this.userMapper = userMapper;
        this.userPieceSkinUnlockMapper = userPieceSkinUnlockMapper;
        this.shopPricingService = shopPricingService;
    }

    /** 是否可用积分兑换（shop 配置了一次性积分价） */
    public boolean isRedeemableSkinId(String skinId) {
        return shopPricingService.findOneTimeUnlockPointsCost(skinId).isPresent();
    }

    public int costPointsFor(String skinId) {
        return shopPricingService.findOneTimeUnlockPointsCost(skinId).orElse(0);
    }

    /**
     * @return 兑换结果；已拥有时 {@link PieceSkinRedeemResponse#isAlreadyOwned()} 为 true，不扣积分。
     */
    @Transactional
    public PieceSkinRedeemResult redeemWithPoints(long userId, String skinId) {
        Optional<Integer> costOpt = shopPricingService.findOneTimeUnlockPointsCost(skinId);
        if (!costOpt.isPresent()) {
            return PieceSkinRedeemResult.invalidSkin();
        }
        int cost = costOpt.get();
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
