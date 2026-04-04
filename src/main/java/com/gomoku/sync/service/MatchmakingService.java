package com.gomoku.sync.service;

import com.gomoku.sync.api.dto.RandomMatchResponse;
import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 随机匹配：FIFO 队列中存放「仅黑方、尚无白方」的房间；新玩家优先与队首房间配对。
 */
@Service
public class MatchmakingService {

    private final RoomService roomService;
    private final UserMapper userMapper;
    private final Deque<String> waitingRoomIds = new ArrayDeque<>();
    private final Object lock = new Object();

    public MatchmakingService(RoomService roomService, UserMapper userMapper) {
        this.roomService = roomService;
        this.userMapper = userMapper;
    }

    public RandomMatchResponse enter(long userId) {
        synchronized (lock) {
            while (true) {
                String roomId = waitingRoomIds.peekFirst();
                if (roomId == null) {
                    break;
                }
                RoomService.JoinResult jr = roomService.joinRoom(roomId, userId);
                if (jr.isOk()) {
                    waitingRoomIds.pollFirst();
                    GameRoom room = roomService.getRoom(roomId);
                    return new RandomMatchResponse("guest", roomId, null, jr.getWhiteToken(), room.getSize());
                }
                waitingRoomIds.pollFirst();
                if ("ROOM_NOT_FOUND".equals(jr.getError())) {
                    roomService.removeRoomIfExists(roomId);
                }
                // ROOM_FULL / SAME_USER：移出队列，不删房间
            }
            GameRoom room = roomService.createRoom(userId);
            waitingRoomIds.addLast(room.getRoomId());
            return new RandomMatchResponse("host", room.getRoomId(), room.getBlackToken(), null, room.getSize());
        }
    }

    public enum CancelOutcome {
        OK,
        HAS_GUEST,
        NOT_FOUND_OR_BAD_TOKEN
    }

    /**
     * 房主在超时或主动取消时调用：仅当尚无白方加入时可关闭房间。
     */
    public CancelOutcome cancel(String roomId, String blackToken, long blackUserId) {
        synchronized (lock) {
            GameRoom room = roomService.getRoom(roomId);
            if (room == null) {
                waitingRoomIds.remove(roomId);
                return CancelOutcome.NOT_FOUND_OR_BAD_TOKEN;
            }
            if (!room.getBlackToken().equals(blackToken) || room.getBlackUserId() != blackUserId) {
                return CancelOutcome.NOT_FOUND_OR_BAD_TOKEN;
            }
            if (room.hasGuest()) {
                return CancelOutcome.HAS_GUEST;
            }
            waitingRoomIds.remove(roomId);
            roomService.removeRoomIfExists(roomId);
            return CancelOutcome.OK;
        }
    }

    public enum FallbackBotOutcome {
        /** 已从 users 中随机人机入座白方 */
        OK,
        ROOM_NOT_FOUND,
        BAD_TOKEN,
        HAS_GUEST,
        NO_BOTS
    }

    /**
     * 匹配超时：从数据库随机一名人机加入房间（白方），房主仍为黑方。
     */
    public FallbackBotOutcome assignRandomBot(String roomId, String blackToken, long blackUserId) {
        synchronized (lock) {
            GameRoom room = roomService.getRoom(roomId);
            if (room == null) {
                waitingRoomIds.remove(roomId);
                return FallbackBotOutcome.ROOM_NOT_FOUND;
            }
            if (!room.getBlackToken().equals(blackToken) || room.getBlackUserId() != blackUserId) {
                return FallbackBotOutcome.BAD_TOKEN;
            }
            if (room.hasGuest()) {
                return FallbackBotOutcome.HAS_GUEST;
            }
            Long botId = userMapper.selectRandomBotId();
            if (botId == null) {
                return FallbackBotOutcome.NO_BOTS;
            }
            waitingRoomIds.remove(roomId);
            RoomService.JoinResult jr = roomService.joinRoom(roomId, botId);
            if (!jr.isOk()) {
                waitingRoomIds.addLast(roomId);
                if ("ROOM_NOT_FOUND".equals(jr.getError())) {
                    roomService.removeRoomIfExists(roomId);
                }
                return FallbackBotOutcome.ROOM_NOT_FOUND;
            }
            User botUser = userMapper.selectById(botId);
            if (botUser != null) {
                int dmin = Math.max(1, botUser.getBotSearchDepthMin());
                int dmax = Math.max(1, botUser.getBotSearchDepthMax());
                if (dmin > dmax) {
                    int t = dmin;
                    dmin = dmax;
                    dmax = t;
                }
                room.setBotSearchDepthRange(dmin, dmax);
            }
            room.setWhiteIsBot(true);
            return FallbackBotOutcome.OK;
        }
    }
}
