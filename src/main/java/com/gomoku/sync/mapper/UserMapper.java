package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    User selectByOpenid(@Param("openid") String openid);

    User selectById(@Param("id") long id);

    User selectByIdForUpdate(@Param("id") long id);

    int insert(User user);

    int updateByOpenid(User user);

    int updateRatingProfile(User user);
}
