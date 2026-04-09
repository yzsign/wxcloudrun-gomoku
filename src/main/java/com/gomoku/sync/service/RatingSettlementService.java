package com.gomoku.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomoku.sync.api.dto.SettleGameRequest;
import com.gomoku.sync.api.dto.SettleGameResponse;
import com.gomoku.sync.domain.GameRecord;
import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.domain.GameRoomStateSnapshot;
import com.gomoku.sync.domain.RatingChangeLog;
import com.gomoku.sync.domain.RoomParticipant;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.mapper.GameMapper;
import com.gomoku.sync.mapper.RatingChangeLogMapper;
import com.gomoku.sync.mapper.RoomParticipantMapper;
import com.gomoku.sync.mapper.UserMapper;
import com.gomoku.sync.service.rating.DailyEloCap;
import com.gomoku.sync.service.rating.EloRatingCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
public class RatingSettlementService {

    public static final String OUTCOME_BLACK_WIN = "BLACK_WIN";
    public static final String OUTCOME_WHITE_WIN = "WHITE_WIN";
    public static final String OUTCOME_DRAW = "DRAW";

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final int MAX_STEPS = 256;

    private final RoomParticipantMapper roomParticipantMapper;
    private final GameMapper gameMapper;
    private final RatingChangeLogMapper ratingChangeLogMapper;
    private final UserMapper userMapper;
    private final RoomService roomService;
    private final RoomGameStateService roomGameStateService;
    private final ObjectMapper objectMapper;

    public RatingSettlementService(
            RoomParticipantMapper roomParticipantMapper,
            GameMapper gameMapper,
            RatingChangeLogMapper ratingChangeLogMapper,
            UserMapper userMapper,
            RoomService roomService,
            RoomGameStateService roomGameStateService,
            ObjectMapper objectMapper) {
        this.roomParticipantMapper = roomParticipantMapper;
        this.gameMapper = gameMapper;
        this.ratingChangeLogMapper = ratingChangeLogMapper;
        this.userMapper = userMapper;
        this.roomService = roomService;
        this.roomGameStateService = roomGameStateService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SettleGameResponse settle(long callerUserId, SettleGameRequest req) {
        if (req == null || req.getRoomId() == null || req.getRoomId().isEmpty()) {
            throw new IllegalArgumentException("缺少 roomId");
        }
        String outcome = req.getOutcome();
        if (outcome == null
                || !(OUTCOME_BLACK_WIN.equals(outcome)
                || OUTCOME_WHITE_WIN.equals(outcome)
                || OUTCOME_DRAW.equals(outcome))) {
            throw new IllegalArgumentException("outcome 须为 BLACK_WIN / WHITE_WIN / DRAW");
        }
        int steps = req.getTotalSteps();
        if (steps < 0 || steps > MAX_STEPS) {
            throw new IllegalArgumentException("totalSteps 非法");
        }

        int matchRound = req.getMatchRound() != null ? req.getMatchRound() : 1;
        if (matchRound < 1) {
            throw new IllegalArgumentException("matchRound 非法");
        }
        GameRoom live = roomService.getRoom(req.getRoomId());
        if (live != null && live.getMatchRound() != matchRound) {
            throw new IllegalArgumentException("局次不匹配");
        }

        GameRecord existingGame =
                gameMapper.selectByRoomIdAndMatchRound(req.getRoomId(), matchRound);
        if (existingGame != null) {
            if (callerUserId != existingGame.getBlackUserId()
                    && callerUserId != existingGame.getWhiteUserId()) {
                throw new IllegalArgumentException("仅对局双方可提交结算");
            }
            long existingId = existingGame.getId() != null ? existingGame.getId() : 0L;
            return new SettleGameResponse(
                    existingId,
                    existingGame.getBlackEloAfter(),
                    existingGame.getWhiteEloAfter(),
                    existingGame.getBlackEloDelta(),
                    existingGame.getWhiteEloDelta());
        }

        RoomParticipant rp = roomParticipantMapper.selectByRoomId(req.getRoomId());
        if (rp == null || rp.getWhiteUserId() == null) {
            throw new IllegalArgumentException("房间不存在或尚未满员");
        }
        long blackId = rp.getBlackUserId();
        long whiteId = rp.getWhiteUserId();
        if (callerUserId != blackId && callerUserId != whiteId) {
            throw new IllegalArgumentException("仅对局双方可提交结算");
        }

        Long runawayUid = req.getRunawayUserId();
        boolean runaway = runawayUid != null;
        if (OUTCOME_DRAW.equals(outcome)) {
            if (runaway) {
                throw new IllegalArgumentException("和棋不可带 runawayUserId");
            }
        } else if (OUTCOME_BLACK_WIN.equals(outcome)) {
            if (runaway && !Objects.equals(runawayUid, whiteId)) {
                throw new IllegalArgumentException("BLACK_WIN 时逃跑方应为白方");
            }
        } else {
            if (runaway && !Objects.equals(runawayUid, blackId)) {
                throw new IllegalArgumentException("WHITE_WIN 时逃跑方应为黑方");
            }
        }

        LocalDate today = LocalDate.now(SHANGHAI);

        long idLo = Math.min(blackId, whiteId);
        long idHi = Math.max(blackId, whiteId);
        User uLo = userMapper.selectByIdForUpdate(idLo);
        User uHi = userMapper.selectByIdForUpdate(idHi);
        if (uLo == null || uHi == null) {
            throw new IllegalStateException("用户不存在");
        }
        User black = blackId == idLo ? uLo : uHi;
        User white = blackId == idLo ? uHi : uLo;

        if (white.isBot() && callerUserId != blackId) {
            throw new IllegalArgumentException("人机对局仅执黑方人类玩家可提交结算");
        }

        DailyEloCap.rolloverIfNeeded(black, today);
        DailyEloCap.rolloverIfNeeded(white, today);

        int blackEloBefore = black.getEloScore();
        int whiteEloBefore = white.getEloScore();

        int rawBlack;
        int rawWhite;

        if (OUTCOME_DRAW.equals(outcome)) {
            rawBlack = 0;
            rawWhite = 0;
        } else if (OUTCOME_BLACK_WIN.equals(outcome)) {
            boolean forceM1 = runaway && Objects.equals(runawayUid, whiteId);
            rawBlack = EloRatingCalculator.delta(
                    blackEloBefore,
                    whiteEloBefore,
                    1.0,
                    steps,
                    black.getConsecutiveWins(),
                    black.getConsecutiveLosses(),
                    black.isLowTrust(),
                    forceM1);
            rawWhite = EloRatingCalculator.delta(
                    whiteEloBefore,
                    blackEloBefore,
                    0.0,
                    steps,
                    white.getConsecutiveWins(),
                    white.getConsecutiveLosses(),
                    white.isLowTrust(),
                    false);
            if (runaway) {
                rawWhite -= extraRunawayPenalty(white);
            }
            rawBlack += EloRatingCalculator.upsetBonus(blackEloBefore, whiteEloBefore);
        } else {
            boolean forceM1 = runaway && Objects.equals(runawayUid, blackId);
            rawWhite = EloRatingCalculator.delta(
                    whiteEloBefore,
                    blackEloBefore,
                    1.0,
                    steps,
                    white.getConsecutiveWins(),
                    white.getConsecutiveLosses(),
                    white.isLowTrust(),
                    forceM1);
            rawBlack = EloRatingCalculator.delta(
                    blackEloBefore,
                    whiteEloBefore,
                    0.0,
                    steps,
                    black.getConsecutiveWins(),
                    black.getConsecutiveLosses(),
                    black.isLowTrust(),
                    false);
            if (runaway) {
                rawBlack -= extraRunawayPenalty(black);
            }
            rawWhite += EloRatingCalculator.upsetBonus(whiteEloBefore, blackEloBefore);
        }

        DailyEloCap.applyNetChange(black, rawBlack);
        DailyEloCap.applyNetChange(white, rawWhite);

        int blackEloAfter = black.getEloScore();
        int whiteEloAfter = white.getEloScore();
        int blackDeltaAct = blackEloAfter - blackEloBefore;
        int whiteDeltaAct = whiteEloAfter - whiteEloBefore;

        applyStatsAfterGame(black, white, outcome, runaway, runawayUid, steps);

        userMapper.updateRatingProfile(black);
        userMapper.updateRatingProfile(white);

        GameRecord game = new GameRecord();
        game.setRoomId(req.getRoomId());
        game.setMatchRound(matchRound);
        game.setBlackUserId(blackId);
        game.setWhiteUserId(whiteId);
        game.setTotalSteps(steps);
        game.setOutcome(outcome);
        game.setRunawayUserId(runawayUid);
        game.setBlackEloBefore(blackEloBefore);
        game.setWhiteEloBefore(whiteEloBefore);
        game.setBlackEloAfter(blackEloAfter);
        game.setWhiteEloAfter(whiteEloAfter);
        game.setBlackEloDelta(blackDeltaAct);
        game.setWhiteEloDelta(whiteDeltaAct);
        game.setMovesJson(resolveMovesJson(req, matchRound, steps));
        gameMapper.insert(game);
        long gameId = game.getId() != null ? game.getId() : 0L;

        insertLog(blackId, whiteId, req.getRoomId(), blackEloBefore, blackEloAfter, blackDeltaAct, steps, runaway && Objects.equals(runawayUid, blackId));
        insertLog(whiteId, blackId, req.getRoomId(), whiteEloBefore, whiteEloAfter, whiteDeltaAct, steps, runaway && Objects.equals(runawayUid, whiteId));

        return new SettleGameResponse(gameId, blackEloAfter, whiteEloAfter, blackDeltaAct, whiteDeltaAct);
    }

    /**
     * 优先从当前房间快照（含多实例 DB 同步）取手顺；否则使用请求体中的 {@link SettleGameRequest#getMoves()}。
     */
    private String resolveMovesJson(SettleGameRequest req, int matchRound, int totalSteps) {
        if (totalSteps == 0) {
            return null;
        }
        GameRoom live = roomService.getRoom(req.getRoomId());
        if (live != null) {
            roomGameStateService.syncRoomFromDbIfBehind(live);
        }
        if (live != null && live.getMatchRound() == matchRound) {
            List<GameRoomStateSnapshot.MoveRecord> fromRoom = live.toStateSnapshot().getMoves();
            if (fromRoom != null && fromRoom.size() == totalSteps) {
                try {
                    return objectMapper.writeValueAsString(fromRoom);
                } catch (Exception e) {
                    throw new IllegalStateException("序列化棋谱失败", e);
                }
            }
        }
        if (req.getMoves() != null && req.getMoves().size() == totalSteps) {
            try {
                return objectMapper.writeValueAsString(req.getMoves());
            } catch (Exception e) {
                throw new IllegalStateException("序列化棋谱失败", e);
            }
        }
        throw new IllegalArgumentException(
                "无法保存回放：totalSteps 与棋谱手数不一致。请在结算请求中传入 moves（长度须等于 totalSteps），或确保房间状态已同步。");
    }

    private static int extraRunawayPenalty(User loser) {
        if (loser.getNewbieMatchGames() < 20 && loser.getNewbieRunawayTally() < 3) {
            return 0;
        }
        return 10;
    }

    private void applyStatsAfterGame(
            User black,
            User white,
            String outcome,
            boolean runaway,
            Long runawayUid,
            int steps) {

        black.setTotalGames(black.getTotalGames() + 1);
        white.setTotalGames(white.getTotalGames() + 1);

        black.setNewbieMatchGames(black.getNewbieMatchGames() + 1);
        white.setNewbieMatchGames(white.getNewbieMatchGames() + 1);

        if (OUTCOME_DRAW.equals(outcome)) {
            black.setDrawCount(black.getDrawCount() + 1);
            white.setDrawCount(white.getDrawCount() + 1);
        } else if (OUTCOME_BLACK_WIN.equals(outcome)) {
            black.setWinCount(black.getWinCount() + 1);
            black.setConsecutiveWins(black.getConsecutiveWins() + 1);
            black.setConsecutiveLosses(0);
            white.setConsecutiveLosses(white.getConsecutiveLosses() + 1);
            white.setConsecutiveWins(0);
        } else {
            white.setWinCount(white.getWinCount() + 1);
            white.setConsecutiveWins(white.getConsecutiveWins() + 1);
            white.setConsecutiveLosses(0);
            black.setConsecutiveLosses(black.getConsecutiveLosses() + 1);
            black.setConsecutiveWins(0);
        }

        if (runaway && runawayUid != null) {
            if (Objects.equals(runawayUid, black.getId())) {
                black.setRunawayCount(black.getRunawayCount() + 1);
                black.setNewbieRunawayTally(black.getNewbieRunawayTally() + 1);
            } else {
                white.setRunawayCount(white.getRunawayCount() + 1);
                white.setNewbieRunawayTally(white.getNewbieRunawayTally() + 1);
            }
        }

        if (!runaway || !Objects.equals(runawayUid, black.getId())) {
            black.setPlacementFairGames(black.getPlacementFairGames() + 1);
        }
        if (!runaway || !Objects.equals(runawayUid, white.getId())) {
            white.setPlacementFairGames(white.getPlacementFairGames() + 1);
        }

        if (steps >= 15 && !runaway) {
            black.setActivityPoints(black.getActivityPoints() + 10);
            white.setActivityPoints(white.getActivityPoints() + 10);
            if (OUTCOME_BLACK_WIN.equals(outcome)) {
                black.setActivityPoints(black.getActivityPoints() + 5);
            } else if (OUTCOME_WHITE_WIN.equals(outcome)) {
                white.setActivityPoints(white.getActivityPoints() + 5);
            }
        }

        refreshLowTrust(black);
        refreshLowTrust(white);
    }

    private static void refreshLowTrust(User u) {
        int tg = u.getTotalGames();
        if (tg <= 0) {
            u.setLowTrust(false);
            return;
        }
        u.setLowTrust(u.getRunawayCount() * 100 > tg * 15);
    }

    private void insertLog(
            long userId,
            long opponentId,
            String roomId,
            int eloBefore,
            int eloAfter,
            int delta,
            int steps,
            boolean runawayFlag) {
        RatingChangeLog log = new RatingChangeLog();
        log.setUserId(userId);
        log.setRoomId(roomId);
        log.setOpponentUserId(opponentId);
        log.setEloBefore(eloBefore);
        log.setEloAfter(eloAfter);
        log.setDelta(delta);
        log.setTotalSteps(steps);
        log.setRunaway(runawayFlag);
        ratingChangeLogMapper.insert(log);
    }
}
