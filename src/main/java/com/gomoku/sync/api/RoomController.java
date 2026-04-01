package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.CreateRoomResponse;
import com.gomoku.sync.api.dto.JoinRoomResponse;
import com.gomoku.sync.api.dto.RoomResource;
import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * 创建房间（无路径参数、无 body）
     */
    @PostMapping
    public ResponseEntity<CreateRoomResponse> createRoom() {
        GameRoom room = roomService.createRoom();
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
    public ResponseEntity<?> join(@RequestParam("roomId") String roomId) {
        RoomService.JoinResult jr = roomService.joinRoom(roomId);
        if (!jr.isOk()) {
            if ("ROOM_NOT_FOUND".equals(jr.getError())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError("ROOM_NOT_FOUND", "房间不存在"));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("ROOM_FULL", "房间已满"));
        }
        GameRoom room = roomService.getRoom(roomId);
        return ResponseEntity.ok(new JoinRoomResponse(jr.getWhiteToken(), room.getSize()));
    }
}
