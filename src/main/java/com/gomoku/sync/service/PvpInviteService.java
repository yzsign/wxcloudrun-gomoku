package com.gomoku.sync.service;

import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.mapper.SocialFriendshipMapper;
import com.gomoku.sync.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 好友房：房主通过用户 WS 向指定好友推送对局邀请（非微信分享）。
 */
@Service
public class PvpInviteService {

    private final RoomService roomService;
    private final SocialFriendshipMapper friendshipMapper;
    private final UserMapper userMapper;
    private final UserWebSocketPushService pushService;

    public PvpInviteService(
            RoomService roomService,
            SocialFriendshipMapper friendshipMapper,
            UserMapper userMapper,
            UserWebSocketPushService pushService) {
        this.roomService = roomService;
        this.friendshipMapper = friendshipMapper;
        this.userMapper = userMapper;
        this.pushService = pushService;
    }

    /**
     * 校验房主身份、好友关系与房间可加入后，向被邀请人推送 {@code ROOM_INVITE_INCOMING}。
     */
    public void pushInviteToPeer(long hostUserId, long peerUserId, String roomIdRaw) {
        if (hostUserId == peerUserId) {
            throw new IllegalArgumentException("参数非法");
        }
        String roomId = roomIdRaw != null ? roomIdRaw.trim() : "";
        if (roomId.isEmpty()) {
            throw new IllegalArgumentException("缺少 roomId");
        }
        long low = Math.min(hostUserId, peerUserId);
        long high = Math.max(hostUserId, peerUserId);
        if (friendshipMapper.existsPair(low, high) <= 0) {
            throw new IllegalArgumentException("不是好友");
        }
        GameRoom room = roomService.getRoom(roomId);
        if (room == null) {
            throw new IllegalArgumentException("房间不存在");
        }
        synchronized (room) {
            if (room.getBlackUserId() != hostUserId) {
                throw new IllegalArgumentException("仅房主可发起邀请");
            }
            if (room.isRandomMatch()) {
                throw new IllegalArgumentException("该房间不支持好友邀请");
            }
            if (room.isPuzzleRoom()) {
                throw new IllegalArgumentException("该房间不支持此邀请");
            }
            if (room.getWhiteUserId() != null) {
                throw new IllegalArgumentException("房间已满");
            }
        }
        User from = userMapper.selectById(hostUserId);
        String nick = from != null && from.getNickname() != null ? from.getNickname() : "";
        pushService.roomInviteIncoming(peerUserId, hostUserId, nick, roomId);
    }

    /**
     * 被邀请人拒绝时通知房主（用户 WS {@code ROOM_INVITE_DECLINED}）。
     */
    public void notifyDecline(long inviteeUserId, long inviterUserId, String roomIdRaw) {
        if (inviteeUserId == inviterUserId) {
            throw new IllegalArgumentException("参数非法");
        }
        String roomId = roomIdRaw != null ? roomIdRaw.trim() : "";
        if (roomId.isEmpty()) {
            throw new IllegalArgumentException("缺少 roomId");
        }
        long low = Math.min(inviteeUserId, inviterUserId);
        long high = Math.max(inviteeUserId, inviterUserId);
        if (friendshipMapper.existsPair(low, high) <= 0) {
            throw new IllegalArgumentException("不是好友");
        }
        pushService.roomInviteDeclined(inviterUserId, inviteeUserId, roomId);
    }
}
