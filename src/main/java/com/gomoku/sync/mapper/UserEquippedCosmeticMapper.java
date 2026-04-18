package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.UserEquippedCosmetic;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserEquippedCosmeticMapper {

    List<UserEquippedCosmetic> selectByUserId(@Param("userId") long userId);

    int upsert(
            @Param("userId") long userId,
            @Param("category") String category,
            @Param("itemId") String itemId);

    int deleteByUserIdAndCategory(
            @Param("userId") long userId, @Param("category") String category);
}
