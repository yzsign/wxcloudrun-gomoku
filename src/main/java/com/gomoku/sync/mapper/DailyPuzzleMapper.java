package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.DailyPuzzle;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DailyPuzzleMapper {

    DailyPuzzle selectById(@Param("id") long id);

    int countAll();

    Long selectFirstOnlineId();

    int insert(DailyPuzzle puzzle);

    int update(DailyPuzzle puzzle);

    List<DailyPuzzle> selectAllOrderByIdDesc(@Param("limit") int limit);
}
