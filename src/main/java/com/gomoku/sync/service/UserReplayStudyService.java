package com.gomoku.sync.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomoku.sync.api.dto.GameMoveDto;
import com.gomoku.sync.api.dto.UserReplayStudyGetResponse;
import com.gomoku.sync.api.dto.UserReplayStudySaveRequest;
import com.gomoku.sync.domain.Stone;
import com.gomoku.sync.domain.UserReplayStudy;
import com.gomoku.sync.mapper.UserReplayStudyMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserReplayStudyService {

    private static final int MAX_MOVES = 512;

    private final UserReplayStudyMapper userReplayStudyMapper;
    private final ObjectMapper objectMapper;
    private final int boardSize;

    public UserReplayStudyService(
            UserReplayStudyMapper userReplayStudyMapper,
            ObjectMapper objectMapper,
            @Value("${gomoku.board-size:15}") int boardSize) {
        this.userReplayStudyMapper = userReplayStudyMapper;
        this.objectMapper = objectMapper;
        this.boardSize = boardSize;
    }

    public UserReplayStudyGetResponse get(long userId) {
        UserReplayStudy row = userReplayStudyMapper.selectByUserId(userId);
        if (row == null) {
            return UserReplayStudyGetResponse.empty(boardSize);
        }
        try {
            List<GameMoveDto> moves =
                    objectMapper.readValue(row.getMovesJson(), new TypeReference<List<GameMoveDto>>() {});
            int[][] board = objectMapper.readValue(row.getBoardJson(), int[][].class);
            DailyPuzzleAdminService.validateBoardCells(board, boardSize);
            UserReplayStudyGetResponse r = new UserReplayStudyGetResponse();
            r.setHasData(true);
            r.setBoardSize(boardSize);
            r.setMoves(moves);
            r.setReplayStep(row.getReplayStep());
            r.setBoard(board);
            r.setSideToMove(row.getSideToMove());
            r.setSourceGameId(row.getSourceGameId());
            r.setBlackPieceSkinId(PieceSkinSelectionService.sanitizeStoredPieceSkinId(row.getBlackPieceSkinId()));
            r.setWhitePieceSkinId(PieceSkinSelectionService.sanitizeStoredPieceSkinId(row.getWhitePieceSkinId()));
            if (row.getUpdatedAt() != null) {
                r.setUpdatedAtEpochMs(row.getUpdatedAt().getTime());
            }
            return r;
        } catch (IllegalArgumentException e) {
            return UserReplayStudyGetResponse.empty(boardSize);
        } catch (Exception e) {
            return UserReplayStudyGetResponse.empty(boardSize);
        }
    }

    @Transactional
    public void save(long userId, UserReplayStudySaveRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        List<GameMoveDto> moves = req.getMoves();
        if (moves == null) {
            throw new IllegalArgumentException("moves 不能为空");
        }
        if (moves.size() > MAX_MOVES) {
            throw new IllegalArgumentException("棋谱过长");
        }
        int step = req.getReplayStep();
        if (step < 0 || step > moves.size()) {
            throw new IllegalArgumentException("replayStep 非法");
        }
        validateMovesShape(moves);
        int[][] built = buildBoardFromMoves(moves, step);
        int[][] clientBoard = req.getBoard();
        DailyPuzzleAdminService.validateBoardCells(clientBoard, boardSize);
        if (!boardsEqual(built, clientBoard)) {
            throw new IllegalArgumentException("board 与棋谱/replayStep 不一致");
        }
        int stm = req.getSideToMove();
        if (stm != Stone.BLACK && stm != Stone.WHITE) {
            throw new IllegalArgumentException("sideToMove 须为 1 或 2");
        }
        int expected = expectedSideToMoveAfterStep(moves, step);
        if (stm != expected) {
            throw new IllegalArgumentException("sideToMove 与当前步数不匹配");
        }
        Long sourceGameId = req.getSourceGameId();
        if (sourceGameId != null && sourceGameId <= 0) {
            sourceGameId = null;
        }
        String bSkin = PieceSkinSelectionService.sanitizeStoredPieceSkinId(req.getBlackPieceSkinId());
        String wSkin = PieceSkinSelectionService.sanitizeStoredPieceSkinId(req.getWhitePieceSkinId());

        String movesJson;
        String boardJson;
        try {
            movesJson = objectMapper.writeValueAsString(moves);
            boardJson = objectMapper.writeValueAsString(clientBoard);
        } catch (Exception e) {
            throw new IllegalArgumentException("序列化失败");
        }

        UserReplayStudy row = new UserReplayStudy();
        row.setUserId(userId);
        row.setMovesJson(movesJson);
        row.setReplayStep(step);
        row.setBoardJson(boardJson);
        row.setSideToMove(stm);
        row.setSourceGameId(sourceGameId);
        row.setBlackPieceSkinId(bSkin);
        row.setWhitePieceSkinId(wSkin);
        userReplayStudyMapper.upsert(row);
    }

    @Transactional
    public void clear(long userId) {
        userReplayStudyMapper.deleteByUserId(userId);
    }

    private void validateMovesShape(List<GameMoveDto> moves) {
        for (GameMoveDto m : moves) {
            if (m == null) {
                throw new IllegalArgumentException("棋谱含空步");
            }
            if (m.getR() < 0 || m.getR() >= boardSize || m.getC() < 0 || m.getC() >= boardSize) {
                throw new IllegalArgumentException("棋谱坐标越界");
            }
            if (m.getColor() != Stone.BLACK && m.getColor() != Stone.WHITE) {
                throw new IllegalArgumentException("棋谱颜色须为 1 或 2");
            }
        }
    }

    private int[][] buildBoardFromMoves(List<GameMoveDto> moves, int step) {
        int[][] b = new int[boardSize][boardSize];
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                b[r][c] = Stone.EMPTY;
            }
        }
        int n = Math.min(step, moves.size());
        for (int k = 0; k < n; k++) {
            GameMoveDto m = moves.get(k);
            int r = m.getR();
            int c = m.getC();
            if (b[r][c] != Stone.EMPTY) {
                throw new IllegalArgumentException("棋谱冲突：重复落子");
            }
            b[r][c] = m.getColor();
        }
        return b;
    }

    private static boolean boardsEqual(int[][] a, int[][] b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int r = 0; r < a.length; r++) {
            if (a[r] == null || b[r] == null || a[r].length != b[r].length) {
                return false;
            }
            for (int c = 0; c < a[r].length; c++) {
                if (a[r][c] != b[r][c]) {
                    return false;
                }
            }
        }
        return true;
    }

    static int expectedSideToMoveAfterStep(List<GameMoveDto> moves, int step) {
        if (step < moves.size()) {
            return moves.get(step).getColor();
        }
        if (moves.isEmpty()) {
            return Stone.BLACK;
        }
        GameMoveDto last = moves.get(moves.size() - 1);
        return last.getColor() == Stone.BLACK ? Stone.WHITE : Stone.BLACK;
    }
}
