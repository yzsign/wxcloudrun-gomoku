package com.gomoku.sync.mapper;

import org.apache.ibatis.annotations.Param;

public interface DailyPuzzleScheduleMapper {

    Long selectPuzzleIdByDate(@Param("puzzleDate") String puzzleDateYmd);

    int insertIgnore(@Param("puzzleDate") String puzzleDateYmd, @Param("puzzleId") long puzzleId);
}
