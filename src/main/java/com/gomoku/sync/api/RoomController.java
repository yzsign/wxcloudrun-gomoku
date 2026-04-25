package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.CreateRoomResponse;
import com.gomoku.sync.api.dto.JoinRoomResponse;
import com.gomoku.sync.api.dto.RoomResource;
import com.gomoku.sync.api.dto.UserRatingResponse;
import com.gomoku.sync.domain.CosmeticCategory;
import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.domain.Stone;
import com.gomoku.sync.domain.RoomParticipant;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.domain.UserEquippedCosmetic;
import com.gomoku.sync.mapper.RoomParticipantMapper;
import com.gomoku.sync.mapper.UserEquippedCosmeticMapper;
import com.gomoku.sync.mapper.UserMapper;
import com.gomoku.sync.service.ConsumableService;
import com.gomoku.sync.service.FriendWatchService;
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
    private final RoomParticipantMapper roomParticipantMapper;
    private final UserMapper userMapper;
    private final UserEquippedCosmeticMapper userEquippedCosmeticMapper;
    private final ConsumableService consumableService;
    private final FriendWatchService friendWatchService;

    public RoomController(
            RoomService roomService,
            SessionJwtService sessionJwtService,
            RoomParticipantMapper roomParticipantMapper,
            UserMapper userMapper,
            UserEquippedCosmeticMapper userEquippedCosmeticMapper,
            ConsumableService consumableService,
            FriendWatchService friendWatchService) {
        this.roomService = roomService;
        this.sessionJwtService = sessionJwtService;
        this.roomParticipantMapper = roomParticipantMapper;
        this.userMapper = userMapper;
        this.userEquippedCosmeticMapper = userEquippedCosmeticMapper;
        this.consumableService = consumableService;
        this.friendWatchService = friendWatchService;
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
    /**
     * 好友在 PVP 对局中时，为当前登录用户发放观战票（与残局房房主旁观不同）；连 WS 时用 watchToken 作 token 参数。
     */
    @PostMapping("/friend-watch")
    public ResponseEntity<?> friendWatch(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("peerUserId") long peerUserId) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录（需 Authorization: Bearer sessionToken）"));
        }
        FriendWatchService.IssueOutcome out =
                friendWatchService.issueForPeer(uid.get(), peerUserId);
        switch (out.result) {
            case OK:
                return ResponseEntity.ok(out.ok);
            case NOT_FRIENDS:
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiError("NOT_FRIENDS", "仅可观看好友对局"));
            case NOT_IN_GAME:
            case ROOM_GONE:
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                                new ApiError(
                                        "NOT_IN_GAME", "该好友当前不在可观看的对局中"));
            case GAME_OVER:
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiError("GAME_OVER", "对局已结束"));
            case PUZZLE_NOT_SUPPORTED:
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(
                                new ApiError(
                                        "PUZZLE_NOT_SUPPORTED", "该类型对局暂不支持从好友列表观战"));
            case IS_PLAYER_USE_SEAT:
            case WATCHER_IS_PEER:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiError("BAD_REQUEST", "无法观战本局（您已在座位中）"));
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiError("ERROR", "申请观战失败"));
        }
    }

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
            if ("NO_BOTS".equals(jr.getError())) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new ApiError("NO_BOTS", "暂无人机账号，请稍后重试"));
            }
            if ("SAME_USER".equals(jr.getError())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiError("SAME_USER", "不能使用与房主相同的账号加入"));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("ROOM_FULL", "房间已满"));
        }
        GameRoom room = roomService.getRoom(roomId);
        if (room != null && !room.isPuzzleRoom()) {
            roomService.maybeSwapRandomSides(roomId);
            room = roomService.getRoom(roomId);
        }
        int yourColor =
                jr.getGuestColor() != null ? jr.getGuestColor() : Stone.WHITE;
        if (room != null) {
            Integer resolved = room.resolveColorByToken(jr.getGuestToken());
            if (resolved != null) {
                yourColor = resolved;
            }
        }
        return ResponseEntity.ok(
                new JoinRoomResponse(
                        room != null ? room.getSize() : roomService.getBoardSize(),
                        jr.getGuestToken(),
                        yourColor));
    }

    /**
     * 当前房间对手的公开天梯数据（须为房间黑/白之一；需双方已入座）
     */
    @GetMapping("/opponent-rating")
    public ResponseEntity<?> opponentRating(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("roomId") String roomId) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        if (roomId == null || roomId.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("BAD_REQUEST", "缺少 roomId"));
        }
        RoomParticipant rp = roomParticipantMapper.selectByRoomId(roomId);
        if (rp == null || rp.getWhiteUserId() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError("NOT_FOUND", "房间不存在或对手未加入"));
        }
        long blackId = rp.getBlackUserId();
        long whiteId = rp.getWhiteUserId();
        long caller = uid.get();
        boolean asSpectator =
                rp.isPuzzleRoom()
                        && rp.getObserverUserId() != null
                        && caller == rp.getObserverUserId();
        if (caller != blackId && caller != whiteId && !asSpectator) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError("FORBIDDEN", "非本房间玩家"));
        }
        long opponentId;
        if (asSpectator) {
            opponentId = whiteId;
        } else {
            opponentId = caller == blackId ? whiteId : blackId;
        }
        User u = userMapper.selectById(opponentId);
        if (u == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError("NOT_FOUND", "对手用户不存在"));
        }
        String nick = u.getNickname();
        String av = u.getAvatarUrl();
        String pieceSlot = null;
        String boardSkillSlot = null;
        String boardSkillLoveSlot = null;
        for (UserEquippedCosmetic row : userEquippedCosmeticMapper.selectByUserId(opponentId)) {
            if (row == null || row.getCategory() == null) {
                continue;
            }
            if (CosmeticCategory.PIECE_SKIN.equals(row.getCategory())) {
                pieceSlot = row.getItemId();
            } else if (CosmeticCategory.BOARD_SKILL.equals(row.getCategory())) {
                boardSkillSlot = row.getItemId();
            } else if (CosmeticCategory.BOARD_SKILL_LOVE.equals(row.getCategory())) {
                boardSkillLoveSlot = row.getItemId();
            }
        }
        String pieceSkinOut = pieceSlot != null && !pieceSlot.isEmpty() ? pieceSlot : u.getPieceSkinId();
        boolean daggerEquipped =
                boardSkillSlot != null
                        && ConsumableService.KIND_DAGGER.equalsIgnoreCase(boardSkillSlot.trim());
        boolean loveEquipped =
                boardSkillLoveSlot != null
                        && ConsumableService.KIND_LOVE.equalsIgnoreCase(boardSkillLoveSlot.trim());
        UserRatingResponse body =
                new UserRatingResponse(
                        u.getId().longValue(),
                        u.getEloScore(),
                        u.getActivityPoints(),
                        u.getConsecutiveWins(),
                        u.getConsecutiveLosses(),
                        u.getTotalGames(),
                        u.getWinCount(),
                        u.getDrawCount(),
                        u.getRunawayCount(),
                        u.isLowTrust(),
                        u.getPlacementFairGames(),
                        u.getNewbieMatchGames(),
                        nick != null && !nick.isEmpty() ? nick : null,
                        av != null && !av.isEmpty() ? av : null,
                        u.getGender(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        pieceSkinOut,
                        null,
                        daggerEquipped,
                        consumableService.getDaggerCount(opponentId),
                        loveEquipped,
                        consumableService.getLoveCount(opponentId));
        return ResponseEntity.ok(body);
    }
}
