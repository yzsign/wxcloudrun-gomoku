package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.RoomParticipant;
import org.apache.ibatis.annotations.Param;

public interface RoomParticipantMapper {

    int insertBlack(
            @Param("roomId") String roomId,
            @Param("blackUserId") long blackUserId,
            @Param("blackToken") String blackToken);

    int updateWhite(
            @Param("roomId") String roomId,
            @Param("whiteUserId") long whiteUserId,
            @Param("whiteToken") String whiteToken);

    int updateBotMeta(
            @Param("roomId") String roomId,
            @Param("whiteIsBot") boolean whiteIsBot,
            @Param("dmin") int dmin,
            @Param("dmax") int dmax);

    RoomParticipant selectByRoomId(@Param("roomId") String roomId);

    int deleteByRoomId(@Param("roomId") String roomId);
}
