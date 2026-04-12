package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.RoomChatMessageItem;
import com.gomoku.sync.api.dto.RoomChatReportRequest;
import com.gomoku.sync.service.RoomChatService;
import com.gomoku.sync.service.SessionJwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/rooms/chat")
public class RoomChatController {

    private final RoomChatService roomChatService;
    private final SessionJwtService sessionJwtService;

    public RoomChatController(RoomChatService roomChatService, SessionJwtService sessionJwtService) {
        this.roomChatService = roomChatService;
        this.sessionJwtService = sessionJwtService;
    }

    /** GET /api/rooms/chat/messages?roomId=&limit= */
    @GetMapping("/messages")
    public ResponseEntity<?> listMessages(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("roomId") String roomId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        int lim = limit == null ? 80 : limit;
        List<RoomChatMessageItem> items = roomChatService.listMessagesForUser(roomId, uid.get(), lim);
        return ResponseEntity.ok(items);
    }

    /** POST /api/rooms/chat/reports */
    @PostMapping("/reports")
    public ResponseEntity<?> report(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody RoomChatReportRequest body) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        if (body == null
                || body.getRoomId() == null
                || body.getRoomId().isEmpty()
                || body.getMessageId() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("BAD_REQUEST", "缺少 roomId 或 messageId"));
        }
        Optional<String> err =
                roomChatService.tryReport(
                        body.getRoomId(), uid.get(), body.getMessageId(), body.getReason());
        if (err.isPresent()) {
            String c = err.get();
            if ("ROOM_NOT_FOUND".equals(c)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(c, "房间不存在"));
            }
            if ("MESSAGE_NOT_FOUND".equals(c)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(c, "消息不存在"));
            }
            if ("FORBIDDEN".equals(c) || "NOT_OPPONENT_MESSAGE".equals(c)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiError(c, "无法举报该消息"));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(c, c));
        }
        return ResponseEntity.ok().build();
    }
}
