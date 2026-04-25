package com.gomoku.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomoku.sync.api.dto.SettleGameRequest;
import com.gomoku.sync.api.dto.SettleGameResponse;
import com.gomoku.sync.domain.GameRecord;
import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.domain.GameRoomStateSnapshot;
import com.gomoku.sync.domain.RatingChangeLog;
import com.gomoku.sync.domain.RoomParticipant;
import com.gomoku.sync.domain.Stone;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.mapper.GameMapper;
import com.gomoku.sync.mapper.RatingChangeLogMapper;
import com.gomoku.sync.mapper.RoomParticipantMapper;
import com.gomoku.sync.mapper.UserMapper;
import com.gomoku.sync.service.rating.DailyEloCap;
import com.gomoku.sync.service.rating.EloRatingCalculator;
import com.gomoku.sync.service.rating.RatingTitleUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class RatingSettlementService {

    public static final String OUTCOME_BLACK_WIN = "BLACK_WIN";
    public static final String OUTCOME_WHITE_WIN = "WHITE_WIN";
    public static final String OUTCOME_DRAW = "DRAW";

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final int MAX_STEPS = 256;
    /** 与每日残局首次通关 {@link DailyPuzzleService} 一致：残局好友房受邀好友当日首胜时发放 */
    private static final int PUZZLE_FRIEND_FIRST_DAILY_WIN_AP = 20;

    private final RoomParticipantMapper roomParticipantMapper;
    private final GameMapper gameMapper;
    private final RatingChangeLogMapper ratingChangeLogMapper;
    private final UserMapper userMapper;
    private final RoomService roomService;
    private final RoomGameStateService roomGameStateService;
    private final ObjectMapper objectMapper;
    private final PieceSkinSelectionService pieceSkinSelectionService;

    public RatingSettlementService(
            RoomParticipantMapper roomParticipantMapper,
            GameMapper gameMapper,
            RatingChangeLogMapper ratingChangeLogMapper,
            UserMapper userMapper,
            RoomService roomService,
            RoomGameStateService roomGameStateService,
            ObjectMapper objectMapper,
            PieceSkinSelectionService pieceSkinSelectionService) {
        this.roomParticipantMapper = roomParticipantMapper;
        this.gameMapper = gameMapper;
        this.ratingChangeLogMapper = ratingChangeLogMapper;
        this.userMapper = userMapper;
        this.roomService = roomService;
        this.roomGameStateService = roomGameStateService;
        this.objectMapper = objectMapper;
        this.pieceSkinSelectionService = pieceSkinSelectionService;
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
        if (live != null) {
            int liveRound = live.getMatchRound();
            if (matchRound > liveRound) {
                throw new IllegalArgumentException("局次不匹配");
            }
            /**
             * 再来一局后内存房间 matchRound 已递增；补报上一局时 matchRound &lt; liveRound。
             * 若数据库已有更后局次的战绩，则拒绝，避免在已结算更后局次后错误套用较早局次。
             */
            if (matchRound < liveRound
                    && gameMapper.countGamesWithMatchRoundGreaterThan(req.getRoomId(), matchRound) > 0) {
                throw new IllegalArgumentException("已有更后局次的战绩，无法补报该局");
            }
        }

        GameRecord existingGame =
                gameMapper.selectByRoomIdAndMatchRound(req.getRoomId(), matchRound);
        if (existingGame != null) {
            if (callerUserId != existingGame.getBlackUserId()
                    && callerUserId != existingGame.getWhiteUserId()) {
                throw new IllegalArgumentException("仅对局双方可提交结算");
            }
            long existingId = existingGame.getId() != null ? existingGame.getId() : 0L;
            User eb = userMapper.selectById(existingGame.getBlackUserId());
            User ew = userMapper.selectById(existingGame.getWhiteUserId());
            int bApAfter = eb != null ? eb.getActivityPoints() : 0;
            int wApAfter = ew != null ? ew.getActivityPoints() : 0;
            boolean ran = existingGame.getRunawayUserId() != null;
            RoomParticipant rpExisting = roomParticipantMapper.selectByRoomId(req.getRoomId());
            boolean puzzleExisting = rpExisting != null && rpExisting.isPuzzleRoom();
            LocalDate existingDay =
                    existingGame.getCreatedAt() != null
                            ? existingGame
                                    .getCreatedAt()
                                    .toInstant()
                                    .atZone(SHANGHAI)
                                    .toLocalDate()
                            : LocalDate.now(SHANGHAI);
            Long existingGid = existingGame.getId();
            int[] pfExisting =
                    computePuzzleFriendFirstDailyWinApDeltas(
                            rpExisting,
                            existingGame.getOutcome(),
                            existingGame.getTotalSteps(),
                            ran,
                            existingGame.getBlackUserId(),
                            existingGame.getWhiteUserId(),
                            existingGid,
                            existingDay);
            boolean randomExisting =
                    rpExisting != null && rpExisting.isRandomMatch();
            int bApDelta =
                    puzzleExisting
                            ? pfExisting[0]
                            : activityPointsDeltaForSide(
                                    true,
                                    existingGame.getOutcome(),
                                    existingGame.getTotalSteps(),
                                    ran,
                                    randomExisting);
            int wApDelta =
                    puzzleExisting
                            ? pfExisting[1]
                            : activityPointsDeltaForSide(
                                    false,
                                    existingGame.getOutcome(),
                                    existingGame.getTotalSteps(),
                                    ran,
                                    randomExisting);
            boolean cBlack = callerUserId == existingGame.getBlackUserId();
            return new SettleGameResponse(
                    existingId,
                    existingGame.getBlackEloAfter(),
                    existingGame.getWhiteEloAfter(),
                    existingGame.getBlackEloDelta(),
                    existingGame.getWhiteEloDelta(),
                    bApAfter,
                    wApAfter,
                    bApDelta,
                    wApDelta,
                    cBlack ? existingGame.getBlackEloAfter() : existingGame.getWhiteEloAfter(),
                    cBlack ? existingGame.getBlackEloDelta() : existingGame.getWhiteEloDelta(),
                    cBlack ? bApAfter : wApAfter,
                    cBlack ? bApDelta : wApDelta,
                    cBlack);
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

        /**
         * 与内存房间终局状态一致时，以服务器胜者为准，避免客户端把 winner 与 BLACK/WHITE
         * 用 === 比错、或局次/重连时误报，导致黑胜白负反写进天梯。
         */
        if (live != null
                && live.getMatchRound() == matchRound
                && live.isGameOver()) {
            Integer w = live.getWinner();
            if (w == null) {
                outcome = OUTCOME_DRAW;
            } else if (w == Stone.BLACK) {
                outcome = OUTCOME_BLACK_WIN;
            } else if (w == Stone.WHITE) {
                outcome = OUTCOME_WHITE_WIN;
            }
        }

        Long runawayUid = req.getRunawayUserId();
        boolean runaway = runawayUid != null;
        if (OUTCOME_DRAW.equals(outcome)) {
            runawayUid = null;
            runaway = false;
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

        /** 好友等非随机对局：降低有效 K 与越级奖，抑制互刷；随机匹配 1.0 */
        double eloKScale =
                rp.isRandomMatch() ? 1.0 : EloRatingCalculator.FRIEND_NON_RANDOM_K_SCALE;

        int rawBlack;
        int rawWhite;

        /** §4.1 和棋：双方 S=0.5，按公式正常变动；连胜/连败计数在 applyStatsAfterGame 中不变 */
        if (OUTCOME_DRAW.equals(outcome)) {
            rawBlack =
                    EloRatingCalculator.delta(
                            blackEloBefore,
                            whiteEloBefore,
                            0.5,
                            steps,
                            black.getConsecutiveWins(),
                            black.getConsecutiveLosses(),
                            black.isLowTrust(),
                            false,
                            eloKScale);
            rawWhite =
                    EloRatingCalculator.delta(
                            whiteEloBefore,
                            blackEloBefore,
                            0.5,
                            steps,
                            white.getConsecutiveWins(),
                            white.getConsecutiveLosses(),
                            white.isLowTrust(),
                            false,
                            eloKScale);
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
                    forceM1,
                    eloKScale);
            rawWhite = EloRatingCalculator.delta(
                    whiteEloBefore,
                    blackEloBefore,
                    0.0,
                    steps,
                    white.getConsecutiveWins(),
                    white.getConsecutiveLosses(),
                    white.isLowTrust(),
                    false,
                    eloKScale);
            if (runaway) {
                rawWhite -= extraRunawayPenalty(white);
            }
            rawBlack += (int) Math.round(EloRatingCalculator.upsetBonus(blackEloBefore, whiteEloBefore) * eloKScale);
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
                    forceM1,
                    eloKScale);
            rawBlack = EloRatingCalculator.delta(
                    blackEloBefore,
                    whiteEloBefore,
                    0.0,
                    steps,
                    black.getConsecutiveWins(),
                    black.getConsecutiveLosses(),
                    black.isLowTrust(),
                    false,
                    eloKScale);
            if (runaway) {
                rawBlack -= extraRunawayPenalty(black);
            }
            rawWhite += (int) Math.round(EloRatingCalculator.upsetBonus(whiteEloBefore, blackEloBefore) * eloKScale);
        }

        /** 残局好友房：受邀好友未挑战成功时不扣天梯分（仍计胜负与团团积分等逻辑由既有分支处理） */
        if (rp.isPuzzleRoom() && rp.getObserverUserId() != null) {
            Long friendId = puzzleFriendInviteeUserId(rp);
            if (friendId != null && !OUTCOME_DRAW.equals(outcome)) {
                boolean friendWon =
                        (OUTCOME_BLACK_WIN.equals(outcome) && friendId == blackId)
                                || (OUTCOME_WHITE_WIN.equals(outcome) && friendId == whiteId);
                if (!friendWon) {
                    if (friendId == blackId) {
                        rawBlack = 0;
                    } else if (friendId == whiteId) {
                        rawWhite = 0;
                    }
                }
            }
        }

        DailyEloCap.applyNetChange(black, rawBlack);
        DailyEloCap.applyNetChange(white, rawWhite);

        int blackEloAfter = black.getEloScore();
        int whiteEloAfter = white.getEloScore();
        int blackDeltaAct = blackEloAfter - blackEloBefore;
        int whiteDeltaAct = whiteEloAfter - whiteEloBefore;

        int[] pfNew =
                computePuzzleFriendFirstDailyWinApDeltas(
                        rp,
                        outcome,
                        steps,
                        runaway,
                        blackId,
                        whiteId,
                        null,
                        today);
        applyStatsAfterGame(
                black,
                white,
                outcome,
                runaway,
                runawayUid,
                steps,
                rp.isPuzzleRoom(),
                rp.isRandomMatch(),
                pfNew[0],
                pfNew[1]);

        RatingTitleUtil.applyTitleNameFromElo(black);
        RatingTitleUtil.applyTitleNameFromElo(white);
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
        game.setBlackPieceSkinId(pieceSkinSelectionService.resolveEquippedPieceSkinForBroadcast(blackId));
        game.setWhitePieceSkinId(pieceSkinSelectionService.resolveEquippedPieceSkinForBroadcast(whiteId));
        gameMapper.insert(game);
        long gameId = game.getId() != null ? game.getId() : 0L;

        insertLog(blackId, whiteId, req.getRoomId(), blackEloBefore, blackEloAfter, blackDeltaAct, steps, runaway && Objects.equals(runawayUid, blackId));
        insertLog(whiteId, blackId, req.getRoomId(), whiteEloBefore, whiteEloAfter, whiteDeltaAct, steps, runaway && Objects.equals(runawayUid, whiteId));

        boolean puzzleRoom = rp.isPuzzleRoom();
        boolean randomMatchRoom = rp.isRandomMatch();
        int bApDelta =
                puzzleRoom
                        ? pfNew[0]
                        : activityPointsDeltaForSide(true, outcome, steps, runaway, randomMatchRoom);
        int wApDelta =
                puzzleRoom
                        ? pfNew[1]
                        : activityPointsDeltaForSide(false, outcome, steps, runaway, randomMatchRoom);
        boolean cBlack2 = callerUserId == blackId;
        return new SettleGameResponse(
                gameId,
                blackEloAfter,
                whiteEloAfter,
                blackDeltaAct,
                whiteDeltaAct,
                black.getActivityPoints(),
                white.getActivityPoints(),
                bApDelta,
                wApDelta,
                cBlack2 ? blackEloAfter : whiteEloAfter,
                cBlack2 ? blackDeltaAct : whiteDeltaAct,
                cBlack2 ? black.getActivityPoints() : white.getActivityPoints(),
                cBlack2 ? bApDelta : wApDelta,
                cBlack2);
    }

    /**
     * 本局团团积分增量（与 {@link #applyStatsAfterGame} 一致）：
     * 有效局 ≥15 手且非逃跑时——随机匹配：胜 +10、负 +5，和棋双方 +10；好友房等非随机：双方 +10、胜方额外 +5。
     */
    static int activityPointsDeltaForSide(
            boolean forBlack,
            String outcome,
            int steps,
            boolean runaway,
            boolean randomMatchRoom) {
        if (steps < 15 || runaway) {
            return 0;
        }
        if (randomMatchRoom) {
            if (OUTCOME_DRAW.equals(outcome)) {
                return 10;
            }
            if (OUTCOME_BLACK_WIN.equals(outcome)) {
                return forBlack ? 10 : 5;
            }
            if (OUTCOME_WHITE_WIN.equals(outcome)) {
                return forBlack ? 5 : 10;
            }
            return 0;
        }
        int n = 10;
        if (OUTCOME_BLACK_WIN.equals(outcome) && forBlack) {
            n += 5;
        } else if (OUTCOME_WHITE_WIN.equals(outcome) && !forBlack) {
            n += 5;
        }
        return n;
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
            int steps,
            boolean puzzleRoom,
            boolean randomMatchRoom,
            int puzzleFriendExtraBlack,
            int puzzleFriendExtraWhite) {

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

        int bAp =
                puzzleRoom
                        ? puzzleFriendExtraBlack
                        : activityPointsDeltaForSide(true, outcome, steps, runaway, randomMatchRoom);
        int wAp =
                puzzleRoom
                        ? puzzleFriendExtraWhite
                        : activityPointsDeltaForSide(false, outcome, steps, runaway, randomMatchRoom);
        if (bAp > 0) {
            black.setActivityPoints(black.getActivityPoints() + bAp);
        }
        if (wAp > 0) {
            white.setActivityPoints(white.getActivityPoints() + wAp);
        }

        refreshLowTrust(black);
        refreshLowTrust(white);
    }

    /**
     * 残局好友房：受邀好友（非房主）获胜且为当日首场胜局时，胜方 +20 团团积分（与每日残局首次通关一致）。
     *
     * @param beforeGameIdExclusive 已存在对局重放时仅统计 id 小于该值的行；新结算传 null
     */
    private int[] computePuzzleFriendFirstDailyWinApDeltas(
            RoomParticipant rp,
            String outcome,
            int steps,
            boolean runaway,
            long blackId,
            long whiteId,
            Long beforeGameIdExclusive,
            LocalDate calendarDay) {
        int[] out = new int[] {0, 0};
        if (rp == null || !rp.isPuzzleRoom() || rp.getObserverUserId() == null) {
            return out;
        }
        Long friendId = puzzleFriendInviteeUserId(rp);
        if (friendId == null) {
            return out;
        }
        if (runaway || steps < 15 || OUTCOME_DRAW.equals(outcome)) {
            return out;
        }
        long winnerId =
                OUTCOME_BLACK_WIN.equals(outcome)
                        ? blackId
                        : (OUTCOME_WHITE_WIN.equals(outcome) ? whiteId : -1L);
        if (winnerId < 0 || winnerId != friendId) {
            return out;
        }
        LocalDate day = calendarDay != null ? calendarDay : LocalDate.now(SHANGHAI);
        Date start = Date.from(day.atStartOfDay(SHANGHAI).toInstant());
        Date end = Date.from(day.plusDays(1).atStartOfDay(SHANGHAI).toInstant());
        int prior =
                gameMapper.countUserWinsInCreatedRangeBeforeId(
                        friendId, start, end, beforeGameIdExclusive);
        if (prior > 0) {
            return out;
        }
        if (friendId == blackId) {
            out[0] = PUZZLE_FRIEND_FIRST_DAILY_WIN_AP;
        } else {
            out[1] = PUZZLE_FRIEND_FIRST_DAILY_WIN_AP;
        }
        return out;
    }

    /**
     * 残局好友房受邀者：与房主（observer）不同座、且该座非人机的一方。
     */
    private static Long puzzleFriendInviteeUserId(RoomParticipant rp) {
        if (rp == null || !rp.isPuzzleRoom() || rp.getObserverUserId() == null) {
            return null;
        }
        long obs = rp.getObserverUserId();
        if (!rp.isBlackIsBot() && rp.getBlackUserId() != obs) {
            return rp.getBlackUserId();
        }
        Long w = rp.getWhiteUserId();
        if (w != null && !rp.isWhiteIsBot() && !Objects.equals(w, obs)) {
            return w;
        }
        return null;
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
