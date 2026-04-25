package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.DailyPuzzleHintResponse;
import com.gomoku.sync.api.dto.DailyPuzzleSubmitRequest;
import com.gomoku.sync.api.dto.DailyPuzzleSubmitResponse;
import com.gomoku.sync.api.dto.DailyPuzzleTodayResponse;
import com.gomoku.sync.api.dto.PuzzleFriendRoomRequest;
import com.gomoku.sync.api.dto.PuzzleFriendRoomResponse;
import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.service.DailyPuzzleService;
import com.gomoku.sync.service.RoomService;
import com.gomoku.sync.service.SessionJwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/me")
public class MeDailyPuzzleController {

    private final DailyPuzzleService dailyPuzzleService;
    private final SessionJwtService sessionJwtService;
    private final RoomService roomService;

    public MeDailyPuzzleController(
            DailyPuzzleService dailyPuzzleService,
            SessionJwtService sessionJwtService,
            RoomService roomService) {
        this.dailyPuzzleService = dailyPuzzleService;
        this.sessionJwtService = sessionJwtService;
        this.roomService = roomService;
    }

    @GetMapping("/daily-puzzle/today")
    public ResponseEntity<?> today(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        DailyPuzzleTodayResponse body = dailyPuzzleService.getToday(uid.get());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/daily-puzzle/submit")
    public ResponseEntity<?> submit(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody DailyPuzzleSubmitRequest body) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        try {
            DailyPuzzleSubmitResponse res = dailyPuzzleService.submit(uid.get(), body);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiError("UNAVAILABLE", e.getMessage()));
        }
    }

    /**
     * 创建残局好友房：返回 roomId、房主执黑 token、观战 token；好友 POST /api/rooms/join 执白。
     * 新房主应使用 blackToken 连 WebSocket；好友首次连上白方时服务端将棋盘重置为邀请时残局与下一手。
     */
    @PostMapping("/puzzle-friend-room")
    public ResponseEntity<?> createPuzzleFriendRoom(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody PuzzleFriendRoomRequest body) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        if (body == null || body.getBoard() == null) {
            return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", "缺少 board"));
        }
        try {
            GameRoom room = roomService.createPuzzleFriendRoom(uid.get(), body.getBoard(), body.getSideToMove());
            PuzzleFriendRoomResponse res =
                    new PuzzleFriendRoomResponse(
                            room.getRoomId(),
                            room.getBlackToken(),
                            room.getSpectatorToken(),
                            room.getSize());
            return ResponseEntity.status(HttpStatus.CREATED).body(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiError("UNAVAILABLE", e.getMessage()));
        }
    }

    @PostMapping("/daily-puzzle/hint")
    public ResponseEntity<?> hint(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        try {
            DailyPuzzleHintResponse res = dailyPuzzleService.useHint(uid.get());
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiError("UNAVAILABLE", e.getMessage()));
        }
    }
}
