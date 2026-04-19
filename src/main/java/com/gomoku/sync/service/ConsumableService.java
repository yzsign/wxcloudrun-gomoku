package com.gomoku.sync.service;

import com.gomoku.sync.api.dto.ConsumableMutationResponse;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.domain.UserConsumable;
import com.gomoku.sync.mapper.UserConsumableMapper;
import com.gomoku.sync.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsumableService {

    public static final String KIND_DAGGER = "dagger";
    public static final String KIND_LOVE = "love";

    private final UserMapper userMapper;
    private final UserConsumableMapper userConsumableMapper;
    private final ShopPricingService shopPricingService;

    public ConsumableService(
            UserMapper userMapper,
            UserConsumableMapper userConsumableMapper,
            ShopPricingService shopPricingService) {
        this.userMapper = userMapper;
        this.userConsumableMapper = userConsumableMapper;
        this.shopPricingService = shopPricingService;
    }

    public static boolean isKnownKind(String kind) {
        if (kind == null) {
            return false;
        }
        String k = kind.trim().toLowerCase();
        return KIND_DAGGER.equals(k) || KIND_LOVE.equals(k);
    }

    /** 无记录视为 0 */
    public int getDaggerCount(long userId) {
        Integer q = userConsumableMapper.selectQuantityByUserIdAndKind(userId, KIND_DAGGER);
        return q != null ? q : 0;
    }

    /** 无记录视为 0 */
    public int getLoveCount(long userId) {
        Integer q = userConsumableMapper.selectQuantityByUserIdAndKind(userId, KIND_LOVE);
        return q != null ? q : 0;
    }

    private ConsumableMutationResponse mutationBody(long userId, int activityPoints) {
        return new ConsumableMutationResponse(activityPoints, getDaggerCount(userId), getLoveCount(userId));
    }

    /**
     * useOne 扣减后：已有一种库存为内存中的新值，仅再查另一种，避免两次 count 查询。
     */
    private ConsumableMutationResponse mutationBodyAfterUse(
            long userId, int activityPoints, String usedKind, int newQtyForUsedKind) {
        int dagger;
        int love;
        if (KIND_DAGGER.equals(usedKind)) {
            dagger = newQtyForUsedKind;
            Integer q = userConsumableMapper.selectQuantityByUserIdAndKind(userId, KIND_LOVE);
            love = q != null ? q : 0;
        } else {
            love = newQtyForUsedKind;
            Integer q = userConsumableMapper.selectQuantityByUserIdAndKind(userId, KIND_DAGGER);
            dagger = q != null ? q : 0;
        }
        return new ConsumableMutationResponse(activityPoints, dagger, love);
    }

    @Transactional
    public ConsumableResult redeemWithPoints(long userId, String kind) {
        if (kind == null || kind.trim().isEmpty()) {
            return ConsumableResult.invalidKind();
        }
        String k = kind.trim().toLowerCase();
        if (KIND_DAGGER.equals(k)) {
            return redeemOneUnit(userId, KIND_DAGGER, shopPricingService.findPerUnitPointsCostForDagger());
        }
        if (KIND_LOVE.equals(k)) {
            return redeemOneUnit(userId, KIND_LOVE, shopPricingService.findPerUnitPointsCostForLove());
        }
        return ConsumableResult.invalidKind();
    }

    private ConsumableResult redeemOneUnit(long userId, String kind, java.util.Optional<Integer> costOpt) {
        int cost = costOpt.orElse(0);
        if (cost <= 0) {
            return ConsumableResult.invalidKind();
        }
        User u = userMapper.selectByIdForUpdate(userId);
        if (u == null) {
            return ConsumableResult.userMissing();
        }
        if (u.getActivityPoints() < cost) {
            return ConsumableResult.insufficientPoints();
        }
        u.setActivityPoints(u.getActivityPoints() - cost);
        userMapper.updateActivityPoints(u);

        UserConsumable uc = userConsumableMapper.selectByUserIdAndKindForUpdate(userId, kind);
        int next;
        if (uc == null) {
            uc = new UserConsumable();
            uc.setUserId(userId);
            uc.setKind(kind);
            uc.setQuantity(1);
            userConsumableMapper.insert(uc);
            next = 1;
        } else {
            next = Math.max(0, uc.getQuantity()) + 1;
            uc.setQuantity(next);
            userConsumableMapper.updateQuantity(uc);
        }
        return ConsumableResult.ok(mutationBody(userId, u.getActivityPoints()));
    }

    @Transactional
    public ConsumableResult useOne(long userId, String kind) {
        if (kind == null || kind.trim().isEmpty()) {
            return ConsumableResult.invalidKind();
        }
        String k = kind.trim().toLowerCase();
        if (!KIND_DAGGER.equals(k) && !KIND_LOVE.equals(k)) {
            return ConsumableResult.invalidKind();
        }
        /** 不修改 users 表，无需对用户行 FOR UPDATE，减少锁等待与一次重锁读 */
        User u = userMapper.selectById(userId);
        if (u == null) {
            return ConsumableResult.userMissing();
        }
        UserConsumable uc = userConsumableMapper.selectByUserIdAndKindForUpdate(userId, k);
        int qty = uc == null ? 0 : Math.max(0, uc.getQuantity());
        if (qty < 1) {
            return ConsumableResult.noneLeft();
        }
        int next = qty - 1;
        uc.setQuantity(next);
        userConsumableMapper.updateQuantity(uc);
        return ConsumableResult.ok(
                mutationBodyAfterUse(userId, u.getActivityPoints(), k, next));
    }

    public enum ErrorKind {
        INVALID_KIND,
        INSUFFICIENT_POINTS,
        NONE_LEFT,
        NOT_FOUND
    }

    public static final class ConsumableResult {
        private final boolean success;
        private final ConsumableMutationResponse body;
        private final ErrorKind error;

        private ConsumableResult(boolean success, ConsumableMutationResponse body, ErrorKind error) {
            this.success = success;
            this.body = body;
            this.error = error;
        }

        static ConsumableResult ok(ConsumableMutationResponse body) {
            return new ConsumableResult(true, body, null);
        }

        static ConsumableResult invalidKind() {
            return new ConsumableResult(false, null, ErrorKind.INVALID_KIND);
        }

        static ConsumableResult insufficientPoints() {
            return new ConsumableResult(false, null, ErrorKind.INSUFFICIENT_POINTS);
        }

        static ConsumableResult noneLeft() {
            return new ConsumableResult(false, null, ErrorKind.NONE_LEFT);
        }

        static ConsumableResult userMissing() {
            return new ConsumableResult(false, null, ErrorKind.NOT_FOUND);
        }

        public boolean isSuccess() {
            return success;
        }

        public ConsumableMutationResponse getBody() {
            return body;
        }

        public ErrorKind getError() {
            return error;
        }
    }
}
