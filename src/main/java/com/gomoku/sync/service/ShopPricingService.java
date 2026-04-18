package com.gomoku.sync.service;

import com.gomoku.sync.api.dto.ShopCatalogItemDto;
import com.gomoku.sync.api.dto.ShopCatalogResponse;
import com.gomoku.sync.mapper.ShopMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 杂货铺定价：读 shop_items / shop_item_prices；表未部署或暂无行时用 legacy 常量兜底（与旧版硬编码一致）。
 */
@Service
public class ShopPricingService {

    public static final String UNIT_ONE_TIME = "ONE_TIME_UNLOCK";
    public static final String UNIT_PER_UNIT = "PER_UNIT";
    /** 与 shop_items.item_code 中 dagger 一致 */
    public static final String ITEM_CODE_DAGGER = "dagger";
    public static final String ITEM_CODE_LOVE = "love";

    private static final Map<String, Integer> LEGACY_ONE_TIME_POINTS;

    static {
        Map<String, Integer> m = new HashMap<>();
        m.put(PieceSkinRedeemService.SKIN_QINGTAO_LIBAI, 200);
        m.put(PieceSkinRedeemService.THEME_MINT, 200);
        m.put(PieceSkinRedeemService.THEME_INK, 200);
        LEGACY_ONE_TIME_POINTS = Collections.unmodifiableMap(m);
    }

    private static final int LEGACY_DAGGER_PER_UNIT_POINTS = 2;
    private static final int LEGACY_LOVE_PER_UNIT_POINTS = 2;

    private final ShopMapper shopMapper;

    public ShopPricingService(ShopMapper shopMapper) {
        this.shopMapper = shopMapper;
    }

    /** 皮肤 / 棋盘主题一次性积分解锁价 */
    public Optional<Integer> findOneTimeUnlockPointsCost(String itemCode) {
        if (itemCode == null || itemCode.isEmpty()) {
            return Optional.empty();
        }
        Integer db = shopMapper.selectCurrentPointsAmount(itemCode, UNIT_ONE_TIME);
        if (db != null && db > 0) {
            return Optional.of(db);
        }
        Integer leg = LEGACY_ONE_TIME_POINTS.get(itemCode);
        return leg != null ? Optional.of(leg) : Optional.empty();
    }

    /** 消耗品按件兑换价（短剑） */
    public Optional<Integer> findPerUnitPointsCostForDagger() {
        Integer db = shopMapper.selectCurrentPointsAmount(ITEM_CODE_DAGGER, UNIT_PER_UNIT);
        if (db != null && db > 0) {
            return Optional.of(db);
        }
        return Optional.of(LEGACY_DAGGER_PER_UNIT_POINTS);
    }

    /** 消耗品按件兑换价（爱心） */
    public Optional<Integer> findPerUnitPointsCostForLove() {
        Integer db = shopMapper.selectCurrentPointsAmount(ITEM_CODE_LOVE, UNIT_PER_UNIT);
        if (db != null && db > 0) {
            return Optional.of(db);
        }
        return Optional.of(LEGACY_LOVE_PER_UNIT_POINTS);
    }

    public ShopCatalogResponse buildCatalog() {
        List<ShopCatalogItemDto> rows = shopMapper.selectEnabledCatalogWithCurrentPrices();
        if (rows == null) {
            rows = Collections.emptyList();
        }
        return new ShopCatalogResponse(rows);
    }
}
