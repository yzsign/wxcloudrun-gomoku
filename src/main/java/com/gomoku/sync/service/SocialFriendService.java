package com.gomoku.sync.service;

import com.gomoku.sync.api.dto.CreateFriendResponse;
import com.gomoku.sync.api.dto.FriendListItemDto;
import com.gomoku.sync.api.dto.FriendRequestActionResponse;
import com.gomoku.sync.api.dto.FriendStatusResponse;
import com.gomoku.sync.domain.SocialFriendRequest;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.mapper.SocialFriendRemarkMapper;
import com.gomoku.sync.mapper.SocialFriendRequestMapper;
import com.gomoku.sync.mapper.SocialFriendshipMapper;
import com.gomoku.sync.mapper.UserMapper;
import com.gomoku.sync.websocket.UserWebSocketRegistry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import java.util.Date;
import java.util.List;

@Service
public class SocialFriendService {

    private static final int RATE_LIMIT_PER_24H = 3;
    private static final int FRIEND_LIST_MAX = 50;
    private static final int REMARK_MAX_LEN = 64;
    private static final int FRIEND_DM_MAX_LEN = 300;

    private final UserMapper userMapper;
    private final SocialFriendshipMapper friendshipMapper;
    private final SocialFriendRequestMapper requestMapper;
    private final SocialFriendRemarkMapper friendRemarkMapper;
    private final UserWebSocketRegistry userWebSocketRegistry;
    private final UserWebSocketPushService pushService;

    public SocialFriendService(
            UserMapper userMapper,
            SocialFriendshipMapper friendshipMapper,
            SocialFriendRequestMapper requestMapper,
            SocialFriendRemarkMapper friendRemarkMapper,
            UserWebSocketRegistry userWebSocketRegistry,
            UserWebSocketPushService pushService) {
        this.userMapper = userMapper;
        this.friendshipMapper = friendshipMapper;
        this.requestMapper = requestMapper;
        this.friendRemarkMapper = friendRemarkMapper;
        this.userWebSocketRegistry = userWebSocketRegistry;
        this.pushService = pushService;
    }

    @Transactional
    public CreateFriendResponse createRequest(long fromUserId, long targetUserId) {
        if (fromUserId == targetUserId) {
            throw new IllegalArgumentException("不能向自己发起好友申请");
        }
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        long low = Math.min(fromUserId, targetUserId);
        long high = Math.max(fromUserId, targetUserId);
        if (friendshipMapper.existsPair(low, high) > 0) {
            return new CreateFriendResponse("ALREADY_FRIENDS", null);
        }

        SocialFriendRequest pending = requestMapper.selectPendingFromTo(fromUserId, targetUserId);
        pending = ensureFreshPending(pending);
        if (pending != null) {
            return new CreateFriendResponse("PENDING", pending.getId());
        }

        int recent = requestMapper.countCreatedInLast24Hours(fromUserId, targetUserId);
        if (recent >= RATE_LIMIT_PER_24H) {
            return new CreateFriendResponse("RATE_LIMITED", null);
        }

        SocialFriendRequest row = new SocialFriendRequest();
        row.setFromUserId(fromUserId);
        row.setToUserId(targetUserId);
        try {
            requestMapper.insertPending(row);
        } catch (DataIntegrityViolationException e) {
            CreateFriendResponse resolved = tryResolvePendingAfterInsertConflict(fromUserId, targetUserId);
            if (resolved != null) {
                return resolved;
            }
            throw e;
        }

        User from = userMapper.selectById(fromUserId);
        String nick = from != null && from.getNickname() != null ? from.getNickname() : "";
        if (!target.isBot()) {
            pushService.friendRequestIncoming(targetUserId, row.getId(), fromUserId, nick);
        }

        return new CreateFriendResponse("CREATED", row.getId());
    }

    @Transactional
    public FriendRequestActionResponse accept(long actorUserId, long requestId) {
        SocialFriendRequest r = requestMapper.selectById(requestId);
        if (r == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        r = reloadIfPending(r);
        if (r == null || !"PENDING".equals(r.getStatus())) {
            if (r != null && "ACCEPTED".equals(r.getStatus())) {
                return new FriendRequestActionResponse("ALREADY_ACCEPTED");
            }
            throw new IllegalArgumentException("申请已失效或已处理");
        }
        if (r.getToUserId() != actorUserId) {
            throw new IllegalStateException("无权处理该申请");
        }

        long from = r.getFromUserId();
        long to = r.getToUserId();
        long low = Math.min(from, to);
        long high = Math.max(from, to);

        friendshipMapper.insertPair(low, high);

        Date now = new Date();
        requestMapper.updateStatus(r.getId(), "ACCEPTED", now);
        requestMapper.dismissPendingReverse(to, from, now);

        pushService.friendRequestResolved(from, r.getId(), "ACCEPTED", to);
        pushService.friendRequestResolved(to, r.getId(), "ACCEPTED", from);
        pushService.friendshipUpdated(from, to, true);
        pushService.friendshipUpdated(to, from, true);

        return new FriendRequestActionResponse("OK");
    }

    @Transactional
    public FriendRequestActionResponse reject(long actorUserId, long requestId) {
        SocialFriendRequest r = requestMapper.selectById(requestId);
        if (r == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        r = reloadIfPending(r);
        if (r == null || !"PENDING".equals(r.getStatus())) {
            if (r != null && "REJECTED".equals(r.getStatus())) {
                return new FriendRequestActionResponse("ALREADY_REJECTED");
            }
            throw new IllegalArgumentException("申请已失效或已处理");
        }
        if (r.getToUserId() != actorUserId) {
            throw new IllegalStateException("无权处理该申请");
        }

        Date now = new Date();
        requestMapper.updateStatus(r.getId(), "REJECTED", now);

        pushService.friendRequestResolved(r.getFromUserId(), r.getId(), "REJECTED", r.getToUserId());

        return new FriendRequestActionResponse("OK");
    }

    public FriendStatusResponse getFriendStatus(long viewerUserId, long peerUserId) {
        FriendStatusResponse out = new FriendStatusResponse();
        if (viewerUserId == peerUserId) {
            out.setFriends(false);
            return out;
        }

        long low = Math.min(viewerUserId, peerUserId);
        long high = Math.max(viewerUserId, peerUserId);
        out.setFriends(friendshipMapper.existsPair(low, high) > 0);
        if (out.isFriends()) {
            return out;
        }

        SocialFriendRequest outPend = requestMapper.selectPendingFromTo(viewerUserId, peerUserId);
        outPend = ensureFreshPending(outPend);
        if (outPend != null) {
            out.setOutgoingPending(true);
            out.setOutgoingFriendRequestId(outPend.getId());
        }

        SocialFriendRequest inPend = requestMapper.selectPendingFromTo(peerUserId, viewerUserId);
        inPend = ensureFreshPending(inPend);
        if (inPend != null) {
            out.setIncomingPending(true);
            out.setIncomingFriendRequestId(inPend.getId());
        }

        return out;
    }

    /**
     * 并发插入命中唯一约束（含 Spring 包装的 DuplicateKeyException）时，若已存在同向 PENDING 则幂等返回。
     */
    private CreateFriendResponse tryResolvePendingAfterInsertConflict(long fromUserId, long targetUserId) {
        SocialFriendRequest again = requestMapper.selectPendingFromTo(fromUserId, targetUserId);
        again = ensureFreshPending(again);
        if (again != null) {
            return new CreateFriendResponse("PENDING", again.getId());
        }
        return null;
    }

    /**
     * 若已过期则置 EXPIRED 并通知发起人，返回 null；仍为 PENDING 则返回入参。
     */
    private SocialFriendRequest ensureFreshPending(SocialFriendRequest r) {
        if (r == null) {
            return null;
        }
        if (!"PENDING".equals(r.getStatus())) {
            return r;
        }
        if (r.getExpiresAt() == null) {
            return r;
        }
        if (r.getExpiresAt().getTime() > System.currentTimeMillis()) {
            return r;
        }
        int n = requestMapper.expirePendingIfStale(r.getId());
        if (n > 0) {
            pushService.friendRequestResolved(r.getFromUserId(), r.getId(), "EXPIRED", r.getToUserId());
        }
        return null;
    }

    /** selectById 后若仍为 PENDING，按过期规则刷新 */
    private SocialFriendRequest reloadIfPending(SocialFriendRequest r) {
        if (r == null) {
            return null;
        }
        if (!"PENDING".equals(r.getStatus())) {
            return r;
        }
        return ensureFreshPending(r);
    }

    public List<FriendListItemDto> listFriends(long viewerUserId) {
        List<FriendListItemDto> rows = friendshipMapper.listFriendsForUser(viewerUserId, FRIEND_LIST_MAX);
        for (FriendListItemDto row : rows) {
            boolean online = userWebSocketRegistry
                    .getSession(row.getPeerUserId())
                    .map(WebSocketSession::isOpen)
                    .orElse(false);
            row.setOnline(online);
            String nick = row.getNickname() != null ? row.getNickname() : "";
            String rem = row.getRemark() != null ? row.getRemark().trim() : "";
            row.setDisplayName(rem.isEmpty() ? nick : rem);
        }
        return rows;
    }

    @Transactional
    public void unfriend(long actorUserId, long peerUserId) {
        if (actorUserId == peerUserId) {
            throw new IllegalArgumentException("参数非法");
        }
        long low = Math.min(actorUserId, peerUserId);
        long high = Math.max(actorUserId, peerUserId);
        int n = friendshipMapper.deletePair(low, high);
        if (n <= 0) {
            throw new IllegalArgumentException("不是好友");
        }
        friendRemarkMapper.deleteForPair(actorUserId, peerUserId);
        friendRemarkMapper.deleteForPair(peerUserId, actorUserId);
        pushService.friendshipUpdated(actorUserId, peerUserId, false);
        pushService.friendshipUpdated(peerUserId, actorUserId, false);
    }

    @Transactional
    public void setRemark(long actorUserId, long peerUserId, String remarkRaw) {
        if (actorUserId == peerUserId) {
            throw new IllegalArgumentException("参数非法");
        }
        String remark = remarkRaw != null ? remarkRaw.trim() : "";
        if (remark.length() > REMARK_MAX_LEN) {
            throw new IllegalArgumentException("备注最长 " + REMARK_MAX_LEN + " 字");
        }
        long low = Math.min(actorUserId, peerUserId);
        long high = Math.max(actorUserId, peerUserId);
        if (friendshipMapper.existsPair(low, high) <= 0) {
            throw new IllegalArgumentException("不是好友");
        }
        friendRemarkMapper.upsertRemark(actorUserId, peerUserId, remark);
    }

    /**
     * 向好友发送一条私聊消息（经用户 WS 实时送达；不落库）。
     */
    public void sendFriendDirectMessage(long fromUserId, long peerUserId, String textRaw) {
        if (fromUserId == peerUserId) {
            throw new IllegalArgumentException("参数非法");
        }
        String text = textRaw != null ? textRaw.trim() : "";
        if (text.isEmpty()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        if (text.length() > FRIEND_DM_MAX_LEN) {
            throw new IllegalArgumentException("消息最长 " + FRIEND_DM_MAX_LEN + " 字");
        }
        long low = Math.min(fromUserId, peerUserId);
        long high = Math.max(fromUserId, peerUserId);
        if (friendshipMapper.existsPair(low, high) <= 0) {
            throw new IllegalArgumentException("不是好友");
        }
        User from = userMapper.selectById(fromUserId);
        String nick = from != null && from.getNickname() != null ? from.getNickname() : "";
        long sentAt = System.currentTimeMillis();
        pushService.friendDirectMessageIncoming(peerUserId, fromUserId, nick, text, sentAt);
    }
}
