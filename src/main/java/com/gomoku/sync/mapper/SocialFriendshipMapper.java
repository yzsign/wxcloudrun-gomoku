package com.gomoku.sync.mapper;

import com.gomoku.sync.api.dto.FriendListItemDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SocialFriendshipMapper {

    int insertPair(@Param("userLowId") long userLowId, @Param("userHighId") long userHighId);

    int existsPair(@Param("userLowId") long userLowId, @Param("userHighId") long userHighId);

    int deletePair(@Param("userLowId") long userLowId, @Param("userHighId") long userHighId);

    List<FriendListItemDto> listFriendsForUser(@Param("userId") long userId, @Param("limit") int limit);
}
