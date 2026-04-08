package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.UserCheckinState;
import org.apache.ibatis.annotations.Param;

public interface UserCheckinMapper {

    UserCheckinState selectByUserId(@Param("userId") long userId);

    UserCheckinState selectByUserIdForUpdate(@Param("userId") long userId);

    int insertInitial(@Param("userId") long userId);

    int updateState(UserCheckinState state);
}
