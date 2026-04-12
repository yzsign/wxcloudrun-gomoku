package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.RoomChatMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoomChatMessageMapper {

    int insert(RoomChatMessage row);

    RoomChatMessage selectById(@Param("id") long id);

    List<RoomChatMessage> selectByRoomIdAsc(
            @Param("roomId") String roomId, @Param("limit") int limit);
}
