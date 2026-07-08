package com.gomoku.sync.websocket;

import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.service.RoomGameStateService;
import com.gomoku.sync.service.RoomService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 多实例下轮询 MySQL 中的 room_game_state，将远端落子同步到本机 GameRoom 并广播。
 */
@Component
public class RoomGameStatePollTask {

    private final RoomService roomService;
    private final RoomGameStateService roomGameStateService;
    private final GomokuWebSocketHandler gomokuWebSocketHandler;
    private final RoomSessionTracker roomSessionTracker;
    /** 版本 IN 查询单批大小（单机房间多时下调 DB QPS） */
    private final int versionBatchSize;

    public RoomGameStatePollTask(
            RoomService roomService,
            RoomGameStateService roomGameStateService,
            GomokuWebSocketHandler gomokuWebSocketHandler,
            RoomSessionTracker roomSessionTracker,
            @Value("${gomoku.room-sync.version-batch-size:400}") int versionBatchSize) {
        this.roomService = roomService;
        this.roomGameStateService = roomGameStateService;
        this.gomokuWebSocketHandler = gomokuWebSocketHandler;
        this.roomSessionTracker = roomSessionTracker;
        this.versionBatchSize = versionBatchSize;
    }

    @Scheduled(fixedDelayString = "${gomoku.room-sync.poll-ms:150}")
    public void poll() {
        List<String> roomIds = roomSessionTracker.snapshotRoomIds();
        if (roomIds.isEmpty()) {
            return;
        }
        Map<String, Long> versionPeek =
                roomGameStateService.loadStateVersionsForPoll(roomIds, versionBatchSize);
        for (String roomId : roomIds) {
            GameRoom room = roomService.getRoom(roomId);
            if (room == null) {
                continue;
            }
            Long prefetched = versionPeek.get(roomId);
            boolean dbNewer =
                    roomGameStateService.pollApplyIfNewer(roomId, room, prefetched);
            if (room.syncFriendRoomClockPauseForLiveSeats()) {
                roomGameStateService.tryPersist(room);
            }
            if (room.applyClockTimeoutsIfDue()) {
                gomokuWebSocketHandler.cancelPendingBotTasksForRoom(roomId);
                roomGameStateService.tryPersist(room);
                gomokuWebSocketHandler.broadcastState(room);
            } else if (dbNewer) {
                gomokuWebSocketHandler.broadcastState(room);
                gomokuWebSocketHandler.maybePlayBot(room);
            }
        }
    }
}
