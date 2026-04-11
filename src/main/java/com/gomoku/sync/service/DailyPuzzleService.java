package com.gomoku.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomoku.sync.api.dto.DailyPuzzleHintResponse;
import com.gomoku.sync.api.dto.DailyPuzzleSubmitRequest;
import com.gomoku.sync.api.dto.DailyPuzzleSubmitResponse;
import com.gomoku.sync.api.dto.DailyPuzzleTodayResponse;
import com.gomoku.sync.domain.DailyPuzzle;
import com.gomoku.sync.domain.DailyPuzzleReplay;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.domain.UserDailyPuzzle;
import com.gomoku.sync.mapper.DailyPuzzleMapper;
import com.gomoku.sync.mapper.DailyPuzzleScheduleMapper;
import com.gomoku.sync.mapper.UserDailyPuzzleMapper;
import com.gomoku.sync.mapper.UserMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Service
public class DailyPuzzleService {

    public static final String SUBMIT_SOLVED = "SOLVED";
    public static final String SUBMIT_ALREADY = "ALREADY_SOLVED";
    public static final String SUBMIT_INVALID = "INVALID";
    public static final String SUBMIT_NOT_MET = "NOT_MET";

    /** 每个自然日第一次通关当日残局时奖励团团积分（同日再次提交为 ALREADY_SOLVED，不再发放）；不调整天梯分 */
    private static final int FIRST_SOLVE_ACTIVITY_POINTS = 20;

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final DailyPuzzleMapper dailyPuzzleMapper;
    private final DailyPuzzleScheduleMapper dailyPuzzleScheduleMapper;
    private final UserDailyPuzzleMapper userDailyPuzzleMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public DailyPuzzleService(
            DailyPuzzleMapper dailyPuzzleMapper,
            DailyPuzzleScheduleMapper dailyPuzzleScheduleMapper,
            UserDailyPuzzleMapper userDailyPuzzleMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper) {
        this.dailyPuzzleMapper = dailyPuzzleMapper;
        this.dailyPuzzleScheduleMapper = dailyPuzzleScheduleMapper;
        this.userDailyPuzzleMapper = userDailyPuzzleMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    public String todayYmd() {
        return LocalDate.now(ZONE).toString();
    }

    @Transactional
    public DailyPuzzleTodayResponse getToday(long userId) {
        String ymd = todayYmd();
        Long puzzleId = dailyPuzzleScheduleMapper.selectPuzzleIdByDate(ymd);
        if (puzzleId == null) {
            return DailyPuzzleTodayResponse.notScheduled(ymd);
        }
        DailyPuzzle puzzle = dailyPuzzleMapper.selectById(puzzleId);
        if (puzzle == null || puzzle.getStatus() != DailyPuzzle.STATUS_ONLINE) {
            return DailyPuzzleTodayResponse.notScheduled(ymd);
        }

        int[][] board = parseBoard(puzzle.getBoardJson(), puzzle.getBoardSize());
        UserDailyPuzzle row = userDailyPuzzleMapper.selectByUserAndDate(userId, ymd);
        if (row == null) {
            row = new UserDailyPuzzle();
            row.setUserId(userId);
            row.setPuzzleDate(ymd);
            row.setPuzzleId(puzzleId);
            row.setStatus(UserDailyPuzzle.STATUS_IN_PROGRESS);
            row.setAttemptCount(0);
            row.setHintUsed(false);
            userDailyPuzzleMapper.insert(row);
        }

        boolean hasHint = puzzle.getHintText() != null && !puzzle.getHintText().isEmpty();
        String hintOut = null;
        if (row.isHintUsed() && hasHint) {
            hintOut = puzzle.getHintText();
        }

        return new DailyPuzzleTodayResponse(
                true,
                ymd,
                puzzle.getId(),
                puzzle.getTitle(),
                puzzle.getDifficulty(),
                puzzle.getBoardSize(),
                board,
                puzzle.getSideToMove(),
                puzzle.getGoal(),
                puzzle.getMaxUserMoves(),
                row.getStatus(),
                row.getAttemptCount(),
                row.isHintUsed(),
                hintOut,
                hasHint);
    }

    @Transactional
    public DailyPuzzleSubmitResponse submit(long userId, DailyPuzzleSubmitRequest req) {
        if (req == null || req.getMoves() == null) {
            throw new IllegalArgumentException("请提交 moves 数组");
        }
        String ymd = todayYmd();
        Long puzzleId = dailyPuzzleScheduleMapper.selectPuzzleIdByDate(ymd);
        if (puzzleId == null) {
            throw new IllegalStateException("今日未配置残局");
        }
        DailyPuzzle puzzle = dailyPuzzleMapper.selectById(puzzleId);
        if (puzzle == null || puzzle.getStatus() != DailyPuzzle.STATUS_ONLINE) {
            throw new IllegalStateException("题目不可用");
        }

        UserDailyPuzzle row = userDailyPuzzleMapper.selectByUserAndDateForUpdate(userId, ymd);
        if (row == null) {
            UserDailyPuzzle ins = new UserDailyPuzzle();
            ins.setUserId(userId);
            ins.setPuzzleDate(ymd);
            ins.setPuzzleId(puzzleId);
            ins.setStatus(UserDailyPuzzle.STATUS_IN_PROGRESS);
            ins.setAttemptCount(0);
            ins.setHintUsed(false);
            try {
                userDailyPuzzleMapper.insert(ins);
            } catch (DataIntegrityViolationException ignore) {
                // 并发插入：另一方已写入
            }
            row = userDailyPuzzleMapper.selectByUserAndDateForUpdate(userId, ymd);
        }
        if (row == null) {
            throw new IllegalStateException("创建每日残局记录失败");
        }

        if (UserDailyPuzzle.STATUS_SOLVED.equals(row.getStatus())) {
            return new DailyPuzzleSubmitResponse(
                    SUBMIT_ALREADY, true, true, row.getAttemptCount(), 0, null);
        }

        String movesJson;
        try {
            movesJson = objectMapper.writeValueAsString(req.getMoves());
        } catch (Exception e) {
            throw new IllegalArgumentException("手顺序列化失败");
        }

        int[][] board = parseBoard(puzzle.getBoardJson(), puzzle.getBoardSize());
        DailyPuzzleReplay.Result r =
                DailyPuzzleReplay.evaluate(
                        board,
                        puzzle.getBoardSize(),
                        puzzle.getSideToMove(),
                        puzzle.getGoal(),
                        puzzle.getMaxUserMoves(),
                        req.getMoves());

        int nextAttempts = row.getAttemptCount() + 1;
        row.setAttemptCount(nextAttempts);
        row.setLastAttemptMovesJson(movesJson);

        if (r == DailyPuzzleReplay.Result.INVALID) {
            userDailyPuzzleMapper.updateAfterFailedAttempt(row);
            return new DailyPuzzleSubmitResponse(SUBMIT_INVALID, false, false, nextAttempts, 0, null);
        }
        if (r == DailyPuzzleReplay.Result.NOT_MET) {
            userDailyPuzzleMapper.updateAfterFailedAttempt(row);
            return new DailyPuzzleSubmitResponse(SUBMIT_NOT_MET, false, false, nextAttempts, 0, null);
        }

        row.setStatus(UserDailyPuzzle.STATUS_SOLVED);
        row.setBestMovesToWin(req.getMoves().size());
        row.setSolvedAt(new Date());
        userDailyPuzzleMapper.updateSolved(row);

        User user = userMapper.selectByIdForUpdate(userId);
        int apDelta = 0;
        Integer apAfter = null;
        if (user != null) {
            apDelta = FIRST_SOLVE_ACTIVITY_POINTS;
            int nextAp = user.getActivityPoints() + apDelta;
            user.setActivityPoints(nextAp);
            userMapper.updateActivityPoints(user);
            apAfter = nextAp;
        }

        return new DailyPuzzleSubmitResponse(SUBMIT_SOLVED, true, false, nextAttempts, apDelta, apAfter);
    }

    @Transactional
    public DailyPuzzleHintResponse useHint(long userId) {
        String ymd = todayYmd();
        Long puzzleId = dailyPuzzleScheduleMapper.selectPuzzleIdByDate(ymd);
        if (puzzleId == null) {
            throw new IllegalStateException("今日未配置残局");
        }
        DailyPuzzle puzzle = dailyPuzzleMapper.selectById(puzzleId);
        if (puzzle == null || puzzle.getStatus() != DailyPuzzle.STATUS_ONLINE) {
            throw new IllegalStateException("题目不可用");
        }
        if (puzzle.getHintText() == null || puzzle.getHintText().isEmpty()) {
            throw new IllegalArgumentException("本题暂无提示");
        }

        UserDailyPuzzle row = userDailyPuzzleMapper.selectByUserAndDate(userId, ymd);
        if (row == null) {
            row = new UserDailyPuzzle();
            row.setUserId(userId);
            row.setPuzzleDate(ymd);
            row.setPuzzleId(puzzleId);
            row.setStatus(UserDailyPuzzle.STATUS_IN_PROGRESS);
            row.setAttemptCount(0);
            row.setHintUsed(false);
            userDailyPuzzleMapper.insert(row);
        }

        userDailyPuzzleMapper.updateHintUsed(userId, ymd);
        return new DailyPuzzleHintResponse(puzzle.getHintText());
    }

    private int[][] parseBoard(String boardJson, int boardSize) {
        if (boardJson == null || boardJson.isEmpty()) {
            throw new IllegalStateException("题目棋盘数据缺失");
        }
        try {
            int[][] b = objectMapper.readValue(boardJson, int[][].class);
            if (b == null || b.length != boardSize) {
                throw new IllegalStateException("题目棋盘尺寸非法");
            }
            for (int i = 0; i < boardSize; i++) {
                if (b[i] == null || b[i].length != boardSize) {
                    throw new IllegalStateException("题目棋盘尺寸非法");
                }
            }
            return b;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("题目棋盘解析失败");
        }
    }
}
