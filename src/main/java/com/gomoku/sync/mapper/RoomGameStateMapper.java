package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.RoomGameStateRow;
import com.gomoku.sync.domain.RoomStateVersionPeek;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoomGameStateMapper {

    int insertInitial(@Param("roomId") String roomId, @Param("stateJson") String stateJson);

    int updateState(
            @Param("roomId") String roomId,
            @Param("stateJson") String stateJson,
            @Param("expectedVersion") long expectedVersion);

    RoomGameStateRow selectByRoomId(@Param("roomId") String roomId);

    /** 仅读版本号，供轮询快速判断是否有更新（避免反复读 LONGTEXT） */
    Long selectStateVersionByRoomId(@Param("roomId") String roomId);

    /**
     * 单机多房间轮询：一次查询多间房版本，替代「每房一句 SELECT」（房间多时显著减负）。
     * {@code roomIds} 须非空且单次批量不宜过大（调用方分块）。
     */
    List<RoomStateVersionPeek> selectStateVersionsByRoomIds(@Param("roomIds") List<String> roomIds);

    int deleteByRoomId(@Param("roomId") String roomId);
}
