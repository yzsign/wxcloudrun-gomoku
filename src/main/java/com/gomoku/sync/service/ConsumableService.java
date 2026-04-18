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
    /** 与小程序 themes.CONSUMABLE_DAGGER_COST_POINTS 一致：2 团团积分兑换 1 个短剑 */
    public static final int DAGGER_REDEEM_COST_ACTIVITY_POINTS = 2;

    private final UserMapper userMapper;
    private final UserConsumableMapper userConsumableMapper;

    public ConsumableService(UserMapper userMapper, UserConsumableMapper userConsumableMapper) {
        this.userMapper = userMapper;
        this.userConsumableMapper = userConsumableMapper;
    }

    public static boolean isKnownKind(String kind) {
        return KIND_DAGGER.equalsIgnoreCase(kind == null ? "" : kind.trim());
    }

    /** 无记录视为 0 */
    public int getDaggerCount(long userId) {
        Integer q = userConsumableMapper.selectQuantityByUserIdAndKind(userId, KIND_DAGGER);
        return q != null ? q : 0;
    }

    @Transactional
    public ConsumableResult redeemWithPoints(long userId, String kind) {
        if (kind == null || kind.trim().isEmpty()) {
            return ConsumableResult.invalidKind();
        }
        String k = kind.trim().toLowerCase();
        if (!KIND_DAGGER.equals(k)) {
            return ConsumableResult.invalidKind();
        }
        User u = userMapper.selectByIdForUpdate(userId);
        if (u == null) {
            return ConsumableResult.userMissing();
        }
        if (u.getActivityPoints() < DAGGER_REDEEM_COST_ACTIVITY_POINTS) {
            return ConsumableResult.insufficientPoints();
        }
        u.setActivityPoints(u.getActivityPoints() - DAGGER_REDEEM_COST_ACTIVITY_POINTS);
        userMapper.updateActivityPoints(u);

        UserConsumable uc = userConsumableMapper.selectByUserIdAndKindForUpdate(userId, KIND_DAGGER);
        int next;
        if (uc == null) {
            uc = new UserConsumable();
            uc.setUserId(userId);
            uc.setKind(KIND_DAGGER);
            uc.setQuantity(1);
            userConsumableMapper.insert(uc);
            next = 1;
        } else {
            next = Math.max(0, uc.getQuantity()) + 1;
            uc.setQuantity(next);
            userConsumableMapper.updateQuantity(uc);
        }
        return ConsumableResult.ok(new ConsumableMutationResponse(u.getActivityPoints(), next));
    }

    @Transactional
    public ConsumableResult useOne(long userId, String kind) {
        if (kind == null || kind.trim().isEmpty()) {
            return ConsumableResult.invalidKind();
        }
        String k = kind.trim().toLowerCase();
        if (!KIND_DAGGER.equals(k)) {
            return ConsumableResult.invalidKind();
        }
        User u = userMapper.selectByIdForUpdate(userId);
        if (u == null) {
            return ConsumableResult.userMissing();
        }
        UserConsumable uc = userConsumableMapper.selectByUserIdAndKindForUpdate(userId, KIND_DAGGER);
        int qty = uc == null ? 0 : Math.max(0, uc.getQuantity());
        if (qty < 1) {
            return ConsumableResult.noneLeft();
        }
        int next = qty - 1;
        uc.setQuantity(next);
        userConsumableMapper.updateQuantity(uc);
        return ConsumableResult.ok(new ConsumableMutationResponse(u.getActivityPoints(), next));
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
