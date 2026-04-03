package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.GameRecord;
import org.apache.ibatis.annotations.Param;

public interface GameMapper {

    int insert(GameRecord game);

    int countByRoomId(@Param("roomId") String roomId);
}
