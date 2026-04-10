package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.DailyPuzzle;
import org.apache.ibatis.annotations.Param;

public interface DailyPuzzleMapper {

    DailyPuzzle selectById(@Param("id") long id);

    int countAll();

    Long selectFirstOnlineId();

    int insert(DailyPuzzle puzzle);
}
