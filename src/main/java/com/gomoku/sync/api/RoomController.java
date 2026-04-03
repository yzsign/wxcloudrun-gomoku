package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.CreateRoomResponse;
import com.gomoku.sync.api.dto.JoinRoomResponse;
import com.gomoku.sync.api.dto.RoomResource;
import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.service.RoomService;
import com.gomoku.sync.service.SessionJwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Optional;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final SessionJwtService sessionJwtService;

    public RoomController(RoomService roomService, SessionJwtService sessionJwtService) {
        this.roomService = roomService;
        this.sessionJwtService = sessionJwtService;
    }

    /**
     * 创建房间（无路径参数、无 body）
     */
    @PostMapping
    public ResponseEntity<?> createRoom(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录（需 Authorization: Bearer sessionToken）"));
        }
        GameRoom room = roomService.createRoom(uid.get());
        CreateRoomResponse body = new CreateRoomResponse(room.getRoomId(), room.getBlackToken(), room.getSize());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/rooms")
                        .queryParam("roomId", room.getRoomId())
                        .build()
                        .toUri())
                .body(body);
    }

    /**
     * 查询房间：GET /api/rooms?roomId=xxx
     */
    @GetMapping
    public ResponseEntity<RoomResource> getRoom(@RequestParam("roomId") String roomId) {
        GameRoom room = roomService.getRoom(roomId);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new RoomResource(room.getRoomId(), room.getSize(), room.hasGuest()));
    }

    /**
     * 加入房间：POST /api/rooms/join，参数为 roomId（URL 查询参数或 form 均可）
     */
    @PostMapping("/join")
    public ResponseEntity<?> join(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("roomId") String roomId) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录（需 Authorization: Bearer sessionToken）"));
        }
        RoomService.JoinResult jr = roomService.joinRoom(roomId, uid.get());
        if (!jr.isOk()) {
            if ("ROOM_NOT_FOUND".equals(jr.getError())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError("ROOM_NOT_FOUND", "房间不存在"));
            }
            if ("SAME_USER".equals(jr.getError())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiError("SAME_USER", "不能使用与房主相同的账号加入"));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("ROOM_FULL", "房间已满"));
        }
        GameRoom room = roomService.getRoom(roomId);
        return ResponseEntity.ok(new JoinRoomResponse(jr.getWhiteToken(), room.getSize()));
    }
}
