package com.gomoku.sync.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomoku.sync.api.dto.GameMoveDto;
import com.gomoku.sync.api.dto.admin.AdminDailyPuzzleCreateResponse;
import com.gomoku.sync.api.dto.admin.AdminDailyPuzzleDetailResponse;
import com.gomoku.sync.api.dto.admin.AdminDailyPuzzleUpsertRequest;
import com.gomoku.sync.domain.DailyPuzzle;
import com.gomoku.sync.domain.Stone;
import com.gomoku.sync.mapper.DailyPuzzleMapper;
import com.gomoku.sync.mapper.DailyPuzzleScheduleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

@Service
public class DailyPuzzleAdminService {

    private static final int LIST_LIMIT = 300;

    private final DailyPuzzleMapper dailyPuzzleMapper;
    private final DailyPuzzleScheduleMapper dailyPuzzleScheduleMapper;
    private final ObjectMapper objectMapper;

    public DailyPuzzleAdminService(
            DailyPuzzleMapper dailyPuzzleMapper,
            DailyPuzzleScheduleMapper dailyPuzzleScheduleMapper,
            ObjectMapper objectMapper) {
        this.dailyPuzzleMapper = dailyPuzzleMapper;
        this.dailyPuzzleScheduleMapper = dailyPuzzleScheduleMapper;
        this.objectMapper = objectMapper;
    }

    public List<DailyPuzzle> listPuzzles() {
        return dailyPuzzleMapper.selectAllOrderByIdDesc(LIST_LIMIT);
    }

    public AdminDailyPuzzleDetailResponse getPuzzle(long id) {
        DailyPuzzle p = dailyPuzzleMapper.selectById(id);
        if (p == null) {
            throw new IllegalArgumentException("题目不存在");
        }
        int[][] board = parseBoardJson(p.getBoardJson(), p.getBoardSize());
        List<GameMoveDto> solution = parseSolutionMoves(p.getSolutionMovesJson());
        return new AdminDailyPuzzleDetailResponse(
                p.getId(),
                p.getTitle(),
                p.getDifficulty(),
                p.getBoardSize(),
                board,
                p.getSideToMove(),
                p.getGoal(),
                p.getMaxUserMoves(),
                solution,
                p.getHintText(),
                p.getStatus());
    }

    @Transactional
    public AdminDailyPuzzleCreateResponse create(AdminDailyPuzzleUpsertRequest req) {
        validateUpsert(req);
        DailyPuzzle p = toEntity(req);
        dailyPuzzleMapper.insert(p);
        Long newId = p.getId();
        if (newId == null) {
            throw new IllegalStateException("插入题目后未获得 id");
        }
        applyScheduleIfPresent(req.getScheduleDate(), newId);
        return new AdminDailyPuzzleCreateResponse(newId);
    }

    @Transactional
    public void update(long id, AdminDailyPuzzleUpsertRequest req) {
        DailyPuzzle existing = dailyPuzzleMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("题目不存在");
        }
        validateUpsert(req);
        DailyPuzzle p = toEntity(req);
        p.setId(id);
        dailyPuzzleMapper.update(p);
        applyScheduleIfPresent(req.getScheduleDate(), id);
    }

    @Transactional
    public void setSchedule(String puzzleDateYmd, long puzzleId) {
        String d = normalizeScheduleDate(puzzleDateYmd);
        DailyPuzzle p = dailyPuzzleMapper.selectById(puzzleId);
        if (p == null) {
            throw new IllegalArgumentException("puzzleId 不存在");
        }
        dailyPuzzleScheduleMapper.upsert(d, puzzleId);
    }

    private void applyScheduleIfPresent(String scheduleDateRaw, long puzzleId) {
        if (scheduleDateRaw == null) {
            return;
        }
        String trimmed = scheduleDateRaw.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        dailyPuzzleScheduleMapper.upsert(normalizeScheduleDate(trimmed), puzzleId);
    }

    private static String normalizeScheduleDate(String puzzleDateYmd) {
        try {
            return LocalDate.parse(puzzleDateYmd.trim()).toString();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("scheduleDate / puzzleDate 须为 yyyy-MM-dd");
        }
    }

    private DailyPuzzle toEntity(AdminDailyPuzzleUpsertRequest req) {
        DailyPuzzle p = new DailyPuzzle();
        p.setTitle(req.getTitle().trim());
        p.setDifficulty(req.getDifficulty());
        p.setBoardSize(req.getBoardSize());
        try {
            p.setBoardJson(objectMapper.writeValueAsString(req.getBoard()));
        } catch (Exception e) {
            throw new IllegalArgumentException("棋盘序列化失败");
        }
        p.setSideToMove(req.getSideToMove());
        p.setGoal(req.getGoal() != null ? req.getGoal().trim().toUpperCase() : DailyPuzzle.GOAL_WIN);
        p.setMaxUserMoves(req.getMaxUserMoves());
        if (req.getSolutionMoves() != null && !req.getSolutionMoves().isEmpty()) {
            try {
                p.setSolutionMovesJson(objectMapper.writeValueAsString(req.getSolutionMoves()));
            } catch (Exception e) {
                throw new IllegalArgumentException("参考答案序列化失败");
            }
        } else {
            p.setSolutionMovesJson(null);
        }
        p.setHintText(emptyToNull(req.getHintText()));
        p.setStatus(req.getStatus());
        return p;
    }

    private static String emptyToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void validateUpsert(AdminDailyPuzzleUpsertRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("请求体为空");
        }
        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("title 不能为空");
        }
        if (req.getDifficulty() < 1 || req.getDifficulty() > 5) {
            throw new IllegalArgumentException("difficulty 须在 1～5");
        }
        int size = req.getBoardSize();
        if (size < 5 || size > 19) {
            throw new IllegalArgumentException("boardSize 不合法");
        }
        validateBoardCells(req.getBoard(), size);
        int stm = req.getSideToMove();
        if (stm != Stone.BLACK && stm != Stone.WHITE) {
            throw new IllegalArgumentException("sideToMove 须为 1（黑）或 2（白）");
        }
        String g = req.getGoal() != null ? req.getGoal().trim().toUpperCase() : DailyPuzzle.GOAL_WIN;
        if (!DailyPuzzle.GOAL_WIN.equals(g) && !DailyPuzzle.GOAL_DRAW.equals(g)) {
            throw new IllegalArgumentException("goal 须为 WIN 或 DRAW");
        }
        int st = req.getStatus();
        if (st != DailyPuzzle.STATUS_OFFLINE && st != DailyPuzzle.STATUS_ONLINE) {
            throw new IllegalArgumentException("status 须为 0 或 1");
        }
        if (req.getScheduleDate() != null && !req.getScheduleDate().trim().isEmpty()) {
            normalizeScheduleDate(req.getScheduleDate());
        }
    }

    static void validateBoardCells(int[][] board, int size) {
        if (board == null) {
            throw new IllegalArgumentException("board 不能为空");
        }
        if (board.length != size) {
            throw new IllegalArgumentException("board 行数须等于 boardSize");
        }
        for (int r = 0; r < size; r++) {
            if (board[r] == null || board[r].length != size) {
                throw new IllegalArgumentException("board 每行列数须等于 boardSize");
            }
            for (int c = 0; c < size; c++) {
                int v = board[r][c];
                if (v != Stone.EMPTY && v != Stone.BLACK && v != Stone.WHITE) {
                    throw new IllegalArgumentException("棋盘格须为 0、1、2");
                }
            }
        }
    }

    private int[][] parseBoardJson(String boardJson, int boardSize) {
        if (boardJson == null || boardJson.isEmpty()) {
            throw new IllegalStateException("题目棋盘数据缺失");
        }
        try {
            int[][] b = objectMapper.readValue(boardJson, int[][].class);
            validateBoardCells(b, boardSize);
            return b;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("题目棋盘解析失败");
        }
    }

    private List<GameMoveDto> parseSolutionMoves(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<GameMoveDto>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
