package com.gomoku.sync.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserPieceSkinUnlockMapper {

    int countByUserIdAndSkinId(@Param("userId") long userId, @Param("skinId") String skinId);

    List<String> selectSkinIdsByUserId(@Param("userId") long userId);

    int insert(
            @Param("userId") long userId,
            @Param("skinId") String skinId,
            @Param("unlockSource") String unlockSource,
            @Param("pointsSpent") int pointsSpent);
}
