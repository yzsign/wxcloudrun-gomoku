package com.gomoku.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomoku.sync.api.dto.SettleGameRequest;
import com.gomoku.sync.api.dto.SettleGameResponse;
import com.gomoku.sync.domain.GameRecord;
import com.gomoku.sync.domain.RoomParticipant;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.mapper.GameMapper;
import com.gomoku.sync.mapper.RatingChangeLogMapper;
import com.gomoku.sync.mapper.RoomParticipantMapper;
import com.gomoku.sync.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.gomoku.sync.service.RatingSettlementService.OUTCOME_BLACK_WIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingSettlementServiceTest {

    @Mock
    private RoomParticipantMapper roomParticipantMapper;
    @Mock
    private GameMapper gameMapper;
    @Mock
    private RatingChangeLogMapper ratingChangeLogMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RoomService roomService;
    @Mock
    private RoomGameStateService roomGameStateService;
    @Mock
    private PieceSkinSelectionService pieceSkinSelectionService;

    private RatingSettlementService ratingSettlementService;

    @BeforeEach
    void setUp() {
        ratingSettlementService =
                new RatingSettlementService(
                        roomParticipantMapper,
                        gameMapper,
                        ratingChangeLogMapper,
                        userMapper,
                        roomService,
                        roomGameStateService,
                        new ObjectMapper(),
                        pieceSkinSelectionService);
    }

    @Test
    void idempotentSettle_unrankedRoom_returnsZeroActivityPointsDelta() {
        GameRecord existing = new GameRecord();
        existing.setId(99L);
        existing.setRoomId("123456");
        existing.setMatchRound(1);
        existing.setBlackUserId(1L);
        existing.setWhiteUserId(2L);
        existing.setTotalSteps(20);
        existing.setOutcome(OUTCOME_BLACK_WIN);
        existing.setBlackEloBefore(1000);
        existing.setWhiteEloBefore(1000);
        existing.setBlackEloAfter(1000);
        existing.setWhiteEloAfter(1000);
        existing.setBlackEloDelta(0);
        existing.setWhiteEloDelta(0);

        when(gameMapper.selectByRoomIdAndMatchRound("123456", 1)).thenReturn(existing);

        User black = new User();
        black.setId(1L);
        black.setActivityPoints(50);
        User white = new User();
        white.setId(2L);
        white.setActivityPoints(60);
        when(userMapper.selectById(1L)).thenReturn(black);
        when(userMapper.selectById(2L)).thenReturn(white);

        RoomParticipant rp = new RoomParticipant();
        rp.setRanked(false);
        rp.setRandomMatch(false);
        rp.setPuzzleRoom(false);
        when(roomParticipantMapper.selectByRoomId("123456")).thenReturn(rp);

        SettleGameRequest req = new SettleGameRequest();
        req.setRoomId("123456");
        req.setMatchRound(1);
        req.setOutcome(OUTCOME_BLACK_WIN);
        req.setTotalSteps(20);

        SettleGameResponse res = ratingSettlementService.settle(2L, req);

        assertEquals(0, res.getBlackActivityPointsDelta());
        assertEquals(0, res.getWhiteActivityPointsDelta());
        assertEquals(0, res.getCallerActivityPointsDelta());
    }
}
