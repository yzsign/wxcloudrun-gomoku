package com.gomoku.sync.mapper;

import org.apache.ibatis.annotations.Param;

public interface SocialFriendshipMapper {

    int insertPair(@Param("userLowId") long userLowId, @Param("userHighId") long userHighId);

    int existsPair(@Param("userLowId") long userLowId, @Param("userHighId") long userHighId);
}
