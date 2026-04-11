package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.GameHistoryQueryRow;
import com.gomoku.sync.domain.GameRecord;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface GameMapper {

    int insert(GameRecord game);

    int countByRoomIdAndMatchRound(@Param("roomId") String roomId, @Param("matchRound") int matchRound);

    GameRecord selectByRoomIdAndMatchRound(@Param("roomId") String roomId, @Param("matchRound") int matchRound);

    GameRecord selectById(@Param("id") long id);

    List<GameHistoryQueryRow> selectHistoryForUser(
            @Param("userId") long userId,
            @Param("limit") int limit,
            @Param("offset") int offset,
            @Param("resultFilter") String resultFilter);

    /**
     * 在 [startInclusive, endExclusive) 内该用户取胜局数；若 beforeIdExclusive 非空则仅统计 id 小于该值的行（用于当日首胜判定）。
     */
    int countUserWinsInCreatedRangeBeforeId(
            @Param("userId") long userId,
            @Param("startInclusive") Date startInclusive,
            @Param("endExclusive") Date endExclusive,
            @Param("beforeIdExclusive") Long beforeIdExclusive);
}
