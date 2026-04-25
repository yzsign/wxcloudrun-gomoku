package com.gomoku.sync.service;

import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.mapper.SocialFriendshipMapper;
import org.springframework.stereotype.Service;

/**
 * 为好友列表「观战」：非残局 PVP 房下发一次性观战票；WebSocket 用 friendWatchToken 与好友关系验证。
 */
@Service
public class FriendWatchService {

    private final GomokuPlayerPresenceRegistry gomokuPlayerPresenceRegistry;
    private final RoomService roomService;
    private final SocialFriendshipMapper friendshipMapper;

    public FriendWatchService(
            GomokuPlayerPresenceRegistry gomokuPlayerPresenceRegistry,
            RoomService roomService,
            SocialFriendshipMapper friendshipMapper) {
        this.gomokuPlayerPresenceRegistry = gomokuPlayerPresenceRegistry;
        this.roomService = roomService;
        this.friendshipMapper = friendshipMapper;
    }

    public enum IssueResult {
        OK,
        UNAUTH,
        NOT_FRIENDS,
        NOT_IN_GAME,
        ROOM_GONE,
        GAME_OVER,
        PUZZLE_NOT_SUPPORTED,
        IS_PLAYER_USE_SEAT,
        WATCHER_IS_PEER
    }

    public static final class IssueOutcome {
        public final IssueResult result;
        public final com.gomoku.sync.api.dto.FriendWatchTicketResponse ok;

        public IssueOutcome(IssueResult r, com.gomoku.sync.api.dto.FriendWatchTicketResponse body) {
            this.result = r;
            this.ok = body;
        }
    }

    public IssueOutcome issueForPeer(long viewerUserId, long peerUserId) {
        if (viewerUserId <= 0 || peerUserId <= 0) {
            return new IssueOutcome(IssueResult.UNAUTH, null);
        }
        if (viewerUserId == peerUserId) {
            return new IssueOutcome(IssueResult.WATCHER_IS_PEER, null);
        }
        long low = Math.min(viewerUserId, peerUserId);
        long high = Math.max(viewerUserId, peerUserId);
        if (friendshipMapper.existsPair(low, high) <= 0) {
            return new IssueOutcome(IssueResult.NOT_FRIENDS, null);
        }
        String roomId = gomokuPlayerPresenceRegistry.findActiveRoomIdForPlayer(peerUserId);
        if (roomId == null) {
            return new IssueOutcome(IssueResult.NOT_IN_GAME, null);
        }
        GameRoom room = roomService.getRoom(roomId);
        if (room == null) {
            return new IssueOutcome(IssueResult.ROOM_GONE, null);
        }
        if (room.isGameOver()) {
            return new IssueOutcome(IssueResult.GAME_OVER, null);
        }
        if (room.isPuzzleRoom()) {
            return new IssueOutcome(IssueResult.PUZZLE_NOT_SUPPORTED, null);
        }
        if (peerUserId != room.getBlackUserId()
                && (room.getWhiteUserId() == null || peerUserId != room.getWhiteUserId())) {
            return new IssueOutcome(IssueResult.NOT_IN_GAME, null);
        }
        if (viewerUserId == room.getBlackUserId()
                || (room.getWhiteUserId() != null && viewerUserId == room.getWhiteUserId())) {
            return new IssueOutcome(IssueResult.IS_PLAYER_USE_SEAT, null);
        }
        roomService.ensureFriendWatchTokenInMemoryAndDb(room);
        return new IssueOutcome(
                IssueResult.OK,
                new com.gomoku.sync.api.dto.FriendWatchTicketResponse(
                        room.getRoomId(), room.getFriendWatchToken(), room.getSize()));
    }
}
