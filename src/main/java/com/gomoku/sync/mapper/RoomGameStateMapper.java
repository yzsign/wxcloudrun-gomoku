package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.RoomGameStateRow;
import org.apache.ibatis.annotations.Param;

public interface RoomGameStateMapper {

    int insertInitial(@Param("roomId") String roomId, @Param("stateJson") String stateJson);

    int updateState(
            @Param("roomId") String roomId,
            @Param("stateJson") String stateJson,
            @Param("expectedVersion") long expectedVersion);

    RoomGameStateRow selectByRoomId(@Param("roomId") String roomId);

    /** 仅读版本号，供轮询快速判断是否有更新（避免反复读 LONGTEXT） */
    Long selectStateVersionByRoomId(@Param("roomId") String roomId);

    int deleteByRoomId(@Param("roomId") String roomId);
}
