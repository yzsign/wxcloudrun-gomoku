package com.gomoku.sync.mapper;

import org.apache.ibatis.annotations.Param;

public interface SocialFriendRemarkMapper {

    int upsertRemark(
            @Param("ownerUserId") long ownerUserId,
            @Param("peerUserId") long peerUserId,
            @Param("remark") String remark);

    int deleteForPair(
            @Param("ownerUserId") long ownerUserId,
            @Param("peerUserId") long peerUserId);
}
