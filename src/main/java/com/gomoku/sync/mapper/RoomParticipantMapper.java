package com.gomoku.sync.mapper;

import com.gomoku.sync.domain.RoomParticipant;
import org.apache.ibatis.annotations.Param;

public interface RoomParticipantMapper {

    int insertBlack(
            @Param("roomId") String roomId,
            @Param("blackUserId") long blackUserId,
            @Param("blackToken") String blackToken);

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
