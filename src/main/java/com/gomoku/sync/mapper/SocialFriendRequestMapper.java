package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.SocialFriendRequest;
import org.apache.ibatis.annotations.Param;

public interface SocialFriendRequestMapper {

    SocialFriendRequest selectById(@Param("id") long id);

    SocialFriendRequest selectPendingFromTo(
            @Param("fromUserId") long fromUserId, @Param("toUserId") long toUserId);

    int countCreatedInLast24Hours(
            @Param("fromUserId") long fromUserId, @Param("toUserId") long toUserId);

    int insertPending(SocialFriendRequest row);

    int updateStatus(
            @Param("id") long id,
            @Param("status") String status,
            @Param("resolvedAt") java.util.Date resolvedAt);

    /** 互申时关闭反向待处理：from_peer → to_peer 一条 PENDING */
    int dismissPendingReverse(
            @Param("fromPeer") long fromPeer,
            @Param("toPeer") long toPeer,
            @Param("resolvedAt") java.util.Date resolvedAt);

    int expirePendingIfStale(@Param("id") long id);
}
