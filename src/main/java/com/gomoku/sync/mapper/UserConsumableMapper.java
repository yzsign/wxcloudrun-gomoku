package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.UserConsumable;
import org.apache.ibatis.annotations.Param;

public interface UserConsumableMapper {

    /**
     * 无则 null（视作 0 件）
     */
    UserConsumable selectByUserIdAndKindForUpdate(
            @Param("userId") long userId, @Param("kind") String kind);

    /**
     * 快捷读，无行返回 null
     */
    Integer selectQuantityByUserIdAndKind(
            @Param("userId") long userId, @Param("kind") String kind);

    int insert(UserConsumable row);

    int updateQuantity(UserConsumable row);
}
