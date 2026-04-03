package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.RoomParticipant;
import org.apache.ibatis.annotations.Param;

public interface RoomParticipantMapper {

    int insertBlack(@Param("roomId") String roomId, @Param("blackUserId") long blackUserId);

    int updateWhite(@Param("roomId") String roomId, @Param("whiteUserId") long whiteUserId);

    RoomParticipant selectByRoomId(@Param("roomId") String roomId);

    int deleteByRoomId(@Param("roomId") String roomId);
}
