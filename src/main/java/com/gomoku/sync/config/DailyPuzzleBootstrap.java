package com.gomoku.sync.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomoku.sync.domain.DailyPuzzle;
import com.gomoku.sync.domain.Stone;
import com.gomoku.sync.mapper.DailyPuzzleMapper;
import com.gomoku.sync.mapper.DailyPuzzleScheduleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 首启时插入示例残局并填充排期（若当日尚无排期）。生产环境可改为仅执行 migration + 运营录入。
 */
@Component
@Order(100)
public class DailyPuzzleBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DailyPuzzleBootstrap.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final DailyPuzzleMapper dailyPuzzleMapper;
    private final DailyPuzzleScheduleMapper dailyPuzzleScheduleMapper;
    private final ObjectMapper objectMapper;

    public DailyPuzzleBootstrap(
            DailyPuzzleMapper dailyPuzzleMapper,
            DailyPuzzleScheduleMapper dailyPuzzleScheduleMapper,
            ObjectMapper objectMapper) {
        this.dailyPuzzleMapper = dailyPuzzleMapper;
        this.dailyPuzzleScheduleMapper = dailyPuzzleScheduleMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedIfNeeded();
        } catch (Exception e) {
            log.warn("Daily puzzle bootstrap skipped: {}", e.toString());
        }
    }

    private void seedIfNeeded() {
        if (dailyPuzzleMapper.countAll() == 0) {
            DailyPuzzle p = buildSamplePuzzle();
            dailyPuzzleMapper.insert(p);
            log.info("Seeded sample daily_puzzle id={}", p.getId());
        }

        LocalDate today = LocalDate.now(ZONE);
        if (dailyPuzzleScheduleMapper.selectPuzzleIdByDate(today.toString()) != null) {
            return;
        }
        Long puzzleId = dailyPuzzleMapper.selectFirstOnlineId();
        if (puzzleId == null) {
            return;
        }
        LocalDate end = today.plusDays(400);
        for (LocalDate d = today; !d.isAfter(end); d = d.plusDays(1)) {
            dailyPuzzleScheduleMapper.insertIgnore(d.toString(), puzzleId);
        }
        log.info("Seeded daily_puzzle_schedule from {} for puzzle_id={}", today, puzzleId);
    }

    private DailyPuzzle buildSamplePuzzle() {
        int size = 15;
        int[][] b = new int[size][size];
        int r = 7;
        for (int c = 4; c <= 7; c++) {
            b[r][c] = Stone.BLACK;
        }
        String boardJson;
        try {
            boardJson = objectMapper.writeValueAsString(b);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        DailyPuzzle p = new DailyPuzzle();
        p.setTitle("示例：四连一手胜");
        p.setDifficulty(1);
        p.setBoardSize(size);
        p.setBoardJson(boardJson);
        p.setSideToMove(Stone.BLACK);
        p.setGoal(DailyPuzzle.GOAL_WIN);
        p.setMaxUserMoves(null);
        p.setSolutionMovesJson("[{\"r\":7,\"c\":8,\"color\":1}]");
        p.setHintText("第五子在开口一侧连成五。");
        p.setStatus(DailyPuzzle.STATUS_ONLINE);
        return p;
    }
}
