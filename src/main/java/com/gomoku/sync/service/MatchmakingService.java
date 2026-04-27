package com.gomoku.sync.service;

import com.gomoku.sync.ai.BotAiStyle;
import com.gomoku.sync.api.dto.RandomMatchResponse;
import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.mapper.UserMapper;
import com.gomoku.sync.service.rating.RatingTitleUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 随机匹配：队列中存放「仅黑方、尚无白方」的房间。寻位者优先与天梯分更接近的一方配对，其次偏好段位更接近；
 * 两位玩家段位（称号档位 0～11）相差超过 3 档则不会匹配到同一间等待房。分数与段位同样接近时，先入队的房主优先被配对。
 */
@Service
public class MatchmakingService {

    /** 段位序（0～11）之差的绝对值超过此值则不可匹配 */
    private static final int MAX_RANDOM_MATCH_RANK_SPAN = 3;

    private final RoomService roomService;
    private final UserMapper userMapper;
    private final boolean randomSwapSides;
    private final Deque<String> waitingRoomIds = new ArrayDeque<>();
    private final Object lock = new Object();

    public MatchmakingService(
            RoomService roomService,
            UserMapper userMapper,
            @Value("${gomoku.match.random-swap-sides:true}") boolean randomSwapSides) {
        this.roomService = roomService;
        this.userMapper = userMapper;
        this.randomSwapSides = randomSwapSides;
    }

    public RandomMatchResponse enter(long userId) {
        synchronized (lock) {
            /*
             * 幂等：房主重复 POST /match/random 时，若其等待房仍在队列中，直接返回同一房间，
             * 避免下方 join 队首时 SAME_USER 误把该房移出队列导致永远无法匹配。
             */
            for (String rid : new ArrayList<>(waitingRoomIds)) {
                GameRoom r = roomService.getRoom(rid);
                if (r != null
                        && !r.isPuzzleRoom()
                        && r.getBlackUserId() == userId
                        && !r.hasGuest()) {
                    return new RandomMatchResponse(
                            "host", r.getRoomId(), r.getBlackToken(), null, r.getSize(), null);
                }
            }
            User guestUser = userMapper.selectById(userId);
            int guestElo = guestUser != null ? guestUser.getEloScore() : 1200;
            int guestRank = RatingTitleUtil.rankIndexForElo(guestElo);
            while (true) {
                String roomId = selectBestWaitingRoomForGuest(userId, guestElo, guestRank);
                if (roomId == null) {
                    break;
                }
                RoomService.JoinResult jr = roomService.joinRoom(roomId, userId);
                if (jr.isOk()) {
                    waitingRoomIds.remove(roomId);
                    if (randomSwapSides) {
                        roomService.maybeSwapRandomSides(roomId);
                    }
                    GameRoom room = roomService.getRoom(roomId);
                    String yourColor = room.resolveSideColorName(userId);
                    if (yourColor == null) {
                        throw new IllegalStateException(
                                "guest seat not resolved after join, roomId=" + roomId);
                    }
                    return new RandomMatchResponse(
                            "guest",
                            roomId,
                            "BLACK".equals(yourColor) ? room.getBlackToken() : null,
                            "WHITE".equals(yourColor) ? room.getWhiteToken() : null,
                            room.getSize(),
                            yourColor);
                }
                waitingRoomIds.remove(roomId);
                if ("ROOM_NOT_FOUND".equals(jr.getError())) {
                    roomService.removeRoomIfExists(roomId);
                }
            }
            GameRoom room = roomService.createRoom(userId, true);
            waitingRoomIds.addLast(room.getRoomId());
            return new RandomMatchResponse("host", room.getRoomId(), room.getBlackToken(), null, room.getSize(), null);
        }
    }

    /**
     * 在待匹配房间中选一间：段位差在 {@link #MAX_RANDOM_MATCH_RANK_SPAN} 内，且
     * 先按天梯分差最小，再按段位差最小；仍相等则偏好先入队的房间。
     */
    private String selectBestWaitingRoomForGuest(
            long guestUserId, int guestElo, int guestRank) {
        List<String> order = new ArrayList<>(waitingRoomIds);
        String bestRoomId = null;
        long bestEloDiff = Long.MAX_VALUE;
        int bestRankDiff = Integer.MAX_VALUE;
        int bestQueueIndex = Integer.MAX_VALUE;

        for (int i = 0; i < order.size(); i++) {
            String roomId = order.get(i);
            GameRoom r = roomService.getRoom(roomId);
            if (r == null) {
                waitingRoomIds.remove(roomId);
                continue;
            }
            if (r.isPuzzleRoom() || r.hasGuest()) {
                waitingRoomIds.remove(roomId);
                continue;
            }
            if (r.getBlackUserId() == guestUserId) {
                continue;
            }
            User host = userMapper.selectById(r.getBlackUserId());
            int hostElo = host != null ? host.getEloScore() : 1200;
            int hostRank = RatingTitleUtil.rankIndexForElo(hostElo);
            int rankDiff = Math.abs(guestRank - hostRank);
            if (rankDiff > MAX_RANDOM_MATCH_RANK_SPAN) {
                continue;
            }
            long eloDiff = Math.abs((long) guestElo - hostElo);
            if (bestRoomId == null
                    || eloDiff < bestEloDiff
                    || (eloDiff == bestEloDiff && rankDiff < bestRankDiff)
                    || (eloDiff == bestEloDiff
                            && rankDiff == bestRankDiff
                            && i < bestQueueIndex)) {
                bestRoomId = roomId;
                bestEloDiff = eloDiff;
                bestRankDiff = rankDiff;
                bestQueueIndex = i;
            }
        }
        return bestRoomId;
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

    /** {@link #assignRandomBot} 成功时含入座人机，供客户端展示昵称与头像 */
    public static final class AssignRandomBotResult {
        private final FallbackBotOutcome outcome;
        private final User botUser;

        private AssignRandomBotResult(FallbackBotOutcome outcome, User botUser) {
            this.outcome = outcome;
            this.botUser = botUser;
        }

        public static AssignRandomBotResult of(FallbackBotOutcome outcome) {
            return new AssignRandomBotResult(outcome, null);
        }

        public static AssignRandomBotResult ok(User botUser) {
            return new AssignRandomBotResult(FallbackBotOutcome.OK, botUser);
        }

        public FallbackBotOutcome getOutcome() {
            return outcome;
        }

        public User getBotUser() {
            return botUser;
        }
    }

    /**
     * 匹配超时：从数据库随机一名人机加入房间（白方），房主仍为黑方。
     */
    public AssignRandomBotResult assignRandomBot(String roomId, String blackToken, long blackUserId) {
        synchronized (lock) {
            GameRoom room = roomService.getRoom(roomId);
            if (room == null) {
                waitingRoomIds.remove(roomId);
                return AssignRandomBotResult.of(FallbackBotOutcome.ROOM_NOT_FOUND);
            }
            if (!room.getBlackToken().equals(blackToken) || room.getBlackUserId() != blackUserId) {
                return AssignRandomBotResult.of(FallbackBotOutcome.BAD_TOKEN);
            }
            if (room.hasGuest()) {
                return AssignRandomBotResult.of(FallbackBotOutcome.HAS_GUEST);
            }
            Long botId = userMapper.selectRandomBotId();
            if (botId == null) {
                return AssignRandomBotResult.of(FallbackBotOutcome.NO_BOTS);
            }
            waitingRoomIds.remove(roomId);
            RoomService.JoinResult jr = roomService.joinRoom(roomId, botId);
            if (!jr.isOk()) {
                waitingRoomIds.addLast(roomId);
                if ("ROOM_NOT_FOUND".equals(jr.getError())) {
                    roomService.removeRoomIfExists(roomId);
                }
                return AssignRandomBotResult.of(FallbackBotOutcome.ROOM_NOT_FOUND);
            }
            int dmin = 2;
            int dmax = 3;
            User botUser = userMapper.selectById(botId);
            if (botUser != null) {
                dmin = Math.max(1, botUser.getBotSearchDepthMin());
                dmax = Math.max(1, botUser.getBotSearchDepthMax());
                if (dmin > dmax) {
                    int t = dmin;
                    dmin = dmax;
                    dmax = t;
                }
                room.setBotSearchDepthRange(dmin, dmax);
            }
            int styleOrd =
                    BotAiStyle.resolveOrdinal(
                            botUser != null ? botUser.getBotAiStyle() : null);
            room.setBotAiStyleOrdinal(styleOrd);
            room.setWhiteIsBot(true);
            roomService.persistWhiteBotMeta(roomId, dmin, dmax, styleOrd);
            roomService.maybeRandomSwapHumanBotSides(roomId);
            return AssignRandomBotResult.ok(botUser);
        }
    }

    /**
     * 随机一名人机账号的公开资料（用于客户端本地随机兜底展示，不与对局强绑定）。
     */
    public User previewRandomBotProfile() {
        Long botId = userMapper.selectRandomBotId();
        if (botId == null) {
            return null;
        }
        return userMapper.selectById(botId);
    }
}
