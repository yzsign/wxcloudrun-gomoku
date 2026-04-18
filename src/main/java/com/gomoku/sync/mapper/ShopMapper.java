package com.gomoku.sync.mapper;

import com.gomoku.sync.api.dto.ShopCatalogItemDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ShopMapper {

    /**
     * 当前有效价：货币 + 单价类型 + 生效期过滤（与 migration 种子一致时一行）。
     */
    Integer selectCurrentPointsAmount(
            @Param("itemCode") String itemCode,
            @Param("unitType") String unitType);

    List<ShopCatalogItemDto> selectEnabledCatalogWithCurrentPrices();
}
