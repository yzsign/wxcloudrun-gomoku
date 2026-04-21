package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.UserReplayStudy;
import org.apache.ibatis.annotations.Param;

public interface UserReplayStudyMapper {

    UserReplayStudy selectByUserId(@Param("userId") long userId);

    int upsert(UserReplayStudy row);

    int deleteByUserId(@Param("userId") long userId);
}
