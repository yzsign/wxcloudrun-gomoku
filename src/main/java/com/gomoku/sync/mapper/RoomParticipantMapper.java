package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.RoomParticipant;
import org.apache.ibatis.annotations.Param;

public interface RoomParticipantMapper {

    int insertBlack(
            @Param("roomId") String roomId,
            @Param("blackUserId") long blackUserId,
            @Param("blackToken") String blackToken,
            @Param("randomMatch") boolean randomMatch);

    int insertPuzzleBlack(
            @Param("roomId") String roomId,
            @Param("blackUserId") long blackUserId,
            @Param("blackToken") String blackToken,
            @Param("observerUserId") long observerUserId,
            @Param("observerToken") String observerToken,
            @Param("puzzleInitBoardJson") String puzzleInitBoardJson,
            @Param("puzzleSideToMove") int puzzleSideToMove);

    int updateWhite(
            @Param("roomId") String roomId,
            @Param("whiteUserId") long whiteUserId,
            @Param("whiteToken") String whiteToken);

    /**
     * 未设置观战票时写入，供多实例下 WS 节点从 DB 恢复一致；仅影响 NULL/空 行，返回 0 表示他处已设置。
     */
    int updateFriendWatchTokenIfNull(
            @Param("roomId") String roomId, @Param("token") String token);

    /**
     * 残局好友房：下一手为黑时好友入座黑方、白方为人机（原黑座为房主占位，须与 observer 一致）。
     */
    int updatePuzzleJoinFriendAsBlack(
            @Param("roomId") String roomId,
            @Param("priorBlackUserId") long priorBlackUserId,
            @Param("friendUserId") long friendUserId,
            @Param("blackToken") String blackToken,
            @Param("whiteBotUserId") long whiteBotUserId,
            @Param("whiteToken") String whiteToken);

    /** 双方已入座后更新黑/白用户与 token（如随机交换先后手） */
    int updateBothSides(
            @Param("roomId") String roomId,
            @Param("blackUserId") long blackUserId,
            @Param("blackToken") String blackToken,
            @Param("whiteUserId") long whiteUserId,
            @Param("whiteToken") String whiteToken);

    int updateBotMeta(
            @Param("roomId") String roomId,
            @Param("whiteIsBot") boolean whiteIsBot,
            @Param("dmin") int dmin,
            @Param("dmax") int dmax,
            @Param("botAiStyle") int botAiStyle);

    int updatePuzzleRoomBots(
            @Param("roomId") String roomId,
            @Param("whiteIsBot") boolean whiteIsBot,
            @Param("blackIsBot") boolean blackIsBot,
            @Param("dmin") int dmin,
            @Param("dmax") int dmax,
            @Param("botAiStyle") int botAiStyle);

    RoomParticipant selectByRoomId(@Param("roomId") String roomId);

    int deleteByRoomId(@Param("roomId") String roomId);
}
