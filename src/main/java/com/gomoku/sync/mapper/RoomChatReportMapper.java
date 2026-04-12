package com.gomoku.sync.mapper;

import org.apache.ibatis.annotations.Param;

public interface RoomChatReportMapper {

    int insert(
            @Param("roomId") String roomId,
            @Param("messageId") long messageId,
            @Param("reporterUserId") long reporterUserId,
            @Param("reason") String reason);
}
