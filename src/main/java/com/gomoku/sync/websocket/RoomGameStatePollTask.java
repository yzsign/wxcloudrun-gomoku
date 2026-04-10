package com.gomoku.sync.websocket;

import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.service.RoomGameStateService;
import com.gomoku.sync.service.RoomService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 多实例下轮询 MySQL 中的 room_game_state，将远端落子同步到本机 GameRoom 并广播。
 */
@Component
public class RoomGameStatePollTask {

    private final RoomService roomService;
    private final RoomGameStateService roomGameStateService;
    private final GomokuWebSocketHandler gomokuWebSocketHandler;
    private final RoomSessionTracker roomSessionTracker;

    public RoomGameStatePollTask(
            RoomService roomService,
            RoomGameStateService roomGameStateService,
            GomokuWebSocketHandler gomokuWebSocketHandler,
            RoomSessionTracker roomSessionTracker) {
        this.roomService = roomService;
        this.roomGameStateService = roomGameStateService;
        this.gomokuWebSocketHandler = gomokuWebSocketHandler;
        this.roomSessionTracker = roomSessionTracker;
    }

    @Scheduled(fixedDelayString = "${gomoku.room-sync.poll-ms:150}")
    public void poll() {
        for (String roomId : roomSessionTracker.snapshotRoomIds()) {
            GameRoom room = roomService.getRoom(roomId);
            if (room == null) {
                continue;
            }
            boolean dbNewer = roomGameStateService.pollApplyIfNewer(roomId, room);
            if (room.applyClockTimeoutsIfDue()) {
                roomGameStateService.tryPersist(room);
                gomokuWebSocketHandler.broadcastState(room);
            } else if (dbNewer) {
                gomokuWebSocketHandler.broadcastState(room);
                gomokuWebSocketHandler.maybePlayBot(room);
            }
        }
    }
}
