package com.gomoku.sync.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.GameMoveDto;
import com.gomoku.sync.api.dto.GameReplayResponse;
import com.gomoku.sync.domain.GameRecord;
import com.gomoku.sync.mapper.GameMapper;
import com.gomoku.sync.service.SessionJwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 对局回放：读取已结算局中保存的 moves_json。
 */
@RestController
@RequestMapping("/api/games")
public class GameReplayController {

    private final GameMapper gameMapper;
    private final SessionJwtService sessionJwtService;
    private final ObjectMapper objectMapper;
    private final int boardSize;

    public GameReplayController(
            GameMapper gameMapper,
            SessionJwtService sessionJwtService,
            ObjectMapper objectMapper,
            @Value("${gomoku.board-size:15}") int boardSize) {
        this.gameMapper = gameMapper;
        this.sessionJwtService = sessionJwtService;
        this.objectMapper = objectMapper;
        this.boardSize = boardSize;
    }

    /**
     * 按房间与局次查询（对局双方或曾参与者可通过 JWT 访问）。
     */
    @GetMapping("/replay")
    public ResponseEntity<?> replayByRoom(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("roomId") String roomId,
            @RequestParam(value = "matchRound", required = false, defaultValue = "1") int matchRound) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        if (roomId == null || roomId.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", "缺少 roomId"));
        }
        if (matchRound < 1) {
            return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", "matchRound 非法"));
        }
        GameRecord g = gameMapper.selectByRoomIdAndMatchRound(roomId, matchRound);
        if (g == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError("NOT_FOUND", "对局不存在或未结算"));
        }
        if (uid.get() != g.getBlackUserId() && uid.get() != g.getWhiteUserId()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError("FORBIDDEN", "仅对局双方可查看回放"));
        }
        return ResponseEntity.ok(toResponse(g));
    }

    @GetMapping("/{gameId}/replay")
    public ResponseEntity<?> replayById(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("gameId") long gameId) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        GameRecord g = gameMapper.selectById(gameId);
        if (g == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError("NOT_FOUND", "对局不存在"));
        }
        if (uid.get() != g.getBlackUserId() && uid.get() != g.getWhiteUserId()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError("FORBIDDEN", "仅对局双方可查看回放"));
        }
        return ResponseEntity.ok(toResponse(g));
    }

    private GameReplayResponse toResponse(GameRecord g) {
        List<GameMoveDto> moves = parseMoves(g.getMovesJson());
        GameReplayResponse r = new GameReplayResponse();
        r.setGameId(g.getId() != null ? g.getId() : 0L);
        r.setRoomId(g.getRoomId());
        r.setMatchRound(g.getMatchRound());
        r.setBoardSize(boardSize);
        r.setBlackUserId(g.getBlackUserId());
        r.setWhiteUserId(g.getWhiteUserId());
        r.setTotalSteps(g.getTotalSteps());
        r.setOutcome(g.getOutcome());
        r.setRunawayUserId(g.getRunawayUserId());
        r.setMoves(moves);
        return r;
    }

    private List<GameMoveDto> parseMoves(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<GameMoveDto>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
