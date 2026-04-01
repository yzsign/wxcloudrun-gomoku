package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.CreateRoomResponse;
import com.gomoku.sync.api.dto.ExistsResponse;
import com.gomoku.sync.api.dto.JoinRoomResponse;
import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public CreateRoomResponse createRoom() {
        GameRoom room = roomService.createRoom();
        return new CreateRoomResponse(room.getRoomId(), room.getBlackToken(), room.getSize());
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> join(@PathVariable String roomId) {
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

    @GetMapping("/{roomId}/exists")
    public ExistsResponse exists(@PathVariable String roomId) {
        return new ExistsResponse(roomService.getRoom(roomId) != null);
    }
}
