package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.UserDailyPuzzle;
import org.apache.ibatis.annotations.Param;

public interface UserDailyPuzzleMapper {

    UserDailyPuzzle selectByUserAndDate(@Param("userId") long userId, @Param("puzzleDate") String puzzleDateYmd);

    int insert(UserDailyPuzzle row);

    int updateAfterFailedAttempt(UserDailyPuzzle row);

    int updateSolved(UserDailyPuzzle row);

    int updateHintUsed(@Param("userId") long userId, @Param("puzzleDate") String puzzleDateYmd);
}
