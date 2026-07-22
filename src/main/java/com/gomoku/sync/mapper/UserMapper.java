package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    User selectByOpenid(@Param("openid") String openid);

    User selectById(@Param("id") long id);

    /** 仅用于对局广播等轻量场景；可能为 null（未设置） */
    String selectPieceSkinIdByUserId(@Param("id") long id);

    User selectByIdForUpdate(@Param("id") long id);

    int insert(User user);

    /** 新用户写入 users 后插入默认天梯行（INSERT IGNORE，与 users 1:1） */
    int insertDefaultRatingProfile(@Param("userId") long userId);

    int updateByOpenid(User user);

    int updateRatingProfile(User user);

    int updateActivityPoints(User user);

    /** 当前佩戴棋子皮肤（须业务层校验已解锁） */
    int updatePieceSkinId(@Param("id") long id, @Param("pieceSkinId") String pieceSkinId);

    /** 随机匹配人机：从 is_bot=1 的用户中任选其一 */
    Long selectRandomBotId();

    /** 随机匹配：优先选用搜索深度较高的人机（max≥5），无则回落全量随机 */
    Long selectRandomMatchBotId();
}
