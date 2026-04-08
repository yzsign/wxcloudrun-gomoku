package com.gomoku.sync.service;

import com.gomoku.sync.api.dto.GameHistoryItemResponse;
import com.gomoku.sync.api.dto.GameHistoryListResponse;
import com.gomoku.sync.domain.GameHistoryQueryRow;
import com.gomoku.sync.mapper.GameMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class GameHistoryService {

    public static final String OUTCOME_BLACK_WIN = "BLACK_WIN";
    public static final String OUTCOME_WHITE_WIN = "WHITE_WIN";
    public static final String OUTCOME_DRAW = "DRAW";

    private static final int MAX_PAGE = 100;

    private final GameMapper gameMapper;

    public GameHistoryService(GameMapper gameMapper) {
        this.gameMapper = gameMapper;
    }

    public GameHistoryListResponse listPage(long userId, int limit, int offset) {
        int lim = Math.min(Math.max(limit, 1), MAX_PAGE);
        int off = Math.max(offset, 0);
        int fetch = lim + 1;
        List<GameHistoryQueryRow> rows = gameMapper.selectHistoryForUser(userId, fetch, off);
        boolean hasMore = rows.size() > lim;
        List<GameHistoryQueryRow> page = hasMore ? rows.subList(0, lim) : rows;

        List<GameHistoryItemResponse> out = new ArrayList<>(page.size());
        for (GameHistoryQueryRow r : page) {
            out.add(toItem(userId, r));
        }
        return new GameHistoryListResponse(out, hasMore);
    }

    private static GameHistoryItemResponse toItem(long userId, GameHistoryQueryRow r) {
        String oppNick = r.getOpponentNickname();
        if (oppNick == null || oppNick.isEmpty()) {
            oppNick = r.isOpponentBot() ? "人机" : "对手";
        }
        Date ca = r.getCreatedAt();
        long endedAt = ca != null ? ca.getTime() : 0L;
        String myResult = resolveMyResult(userId, r.getBlackUserId(), r.getWhiteUserId(), r.getOutcome());
        return new GameHistoryItemResponse(
                r.getId() != null ? r.getId() : 0L,
                r.getRoomId(),
                r.getMatchRound(),
                endedAt,
                oppNick,
                r.isOpponentBot(),
                myResult,
                r.getTotalSteps());
    }

    static String resolveMyResult(long userId, long blackUserId, long whiteUserId, String outcome) {
        if (OUTCOME_DRAW.equals(outcome)) {
            return "DRAW";
        }
        boolean iAmBlack = userId == blackUserId;
        if (OUTCOME_BLACK_WIN.equals(outcome)) {
            return iAmBlack ? "WIN" : "LOSS";
        }
        if (OUTCOME_WHITE_WIN.equals(outcome)) {
            return iAmBlack ? "LOSS" : "WIN";
        }
        return "DRAW";
    }
}
