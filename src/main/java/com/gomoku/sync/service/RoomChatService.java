package com.gomoku.sync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gomoku.sync.api.dto.RoomChatMessageItem;
import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.domain.RoomChatMessage;
import com.gomoku.sync.domain.RoomParticipant;
import com.gomoku.sync.domain.Stone;
import com.gomoku.sync.mapper.RoomParticipantMapper;
import com.gomoku.sync.util.ChatAbusiveTextFilter;
import com.gomoku.sync.util.ChatSensitiveInfoFilter;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 对局内聊天：校验、不落库（仅 WS 实时广播），组装广播 JSON
 */
@Service
public class RoomChatService {

    public static final String KIND_TEXT = "TEXT";
    public static final String KIND_QUICK = "QUICK";
    public static final String KIND_EMOJI = "EMOJI";

    private static final int MAX_TEXT_CODEPOINTS = 30;

    private static final Set<String> QUICK_PHRASES = new HashSet<>();

    static {
        String[] q =
                new String[] {
                    "哈喽～",
                    "这步绝了",
                    "大意了",
                    "稳住别慌",
                    "容我想想",
                    "平局不？",
                    "太强了吧",
                    "输了输了",
                    "你好",
                    "好棋",
                    "失误了",
                    "加油",
                    "和棋",
                    "等等",
                    "承让",
                    "下次再战"
                };
        Collections.addAll(QUICK_PHRASES, q);
    }

    /** 不落库时的消息 id，供客户端去重；与 DB 自增 id 无关联 */
    private static final AtomicLong EPHEMERAL_CHAT_SEQ = new AtomicLong();

    private static long nextEphemeralMessageId() {
        return (System.currentTimeMillis() << 10) | (EPHEMERAL_CHAT_SEQ.incrementAndGet() & 0x3FF);
    }

    private static final Set<String> EMOJIS = new HashSet<>();

    static {
        String[] e =
                new String[] {
                    "\uD83D\uDC4D", // 👍
                    "\uD83D\uDC4C", // 👌
                    "\uD83D\uDE0A", // 😊
                    "\uD83D\uDE05", // 😅
                    "\uD83E\uDD1D", // 🤝
                    "\uD83C\uDFAF", // 🎯
                    "\uD83C\uDF89", // 🎉
                    "\uD83D\uDE14", // 😔
                    "\uD83E\uDD14", // 🤔
                    "\u270B", // ✋
                    "\uD83D\uDC4B", // 👋
                    "\uD83E\uDEE1" // 🫡
                };
        Collections.addAll(EMOJIS, e);
    }

    private final RoomParticipantMapper roomParticipantMapper;
    private final ObjectMapper objectMapper;
    private final WeChatMsgSecCheckService weChatMsgSecCheckService;

    public RoomChatService(
            RoomParticipantMapper roomParticipantMapper,
            ObjectMapper objectMapper,
            WeChatMsgSecCheckService weChatMsgSecCheckService) {
        this.roomParticipantMapper = roomParticipantMapper;
        this.objectMapper = objectMapper;
        this.weChatMsgSecCheckService = weChatMsgSecCheckService;
    }

    public Optional<String> tryReport(
            String roomId, long reporterUserId, long messageId, String reason) {
        RoomParticipant rp = roomParticipantMapper.selectByRoomId(roomId);
        if (rp == null) {
            return Optional.of("ROOM_NOT_FOUND");
        }
        if (!isRoomPlayer(rp, reporterUserId)) {
            return Optional.of("FORBIDDEN");
        }
        return Optional.of("MESSAGE_NOT_FOUND");
    }

    /** 聊天不落库，历史始终为空（保留接口供客户端兼容） */
    public List<RoomChatMessageItem> listMessagesForUser(String roomId, long userId, int limit) {
        RoomParticipant rp = roomParticipantMapper.selectByRoomId(roomId);
        if (rp == null) {
            return Collections.emptyList();
        }
        if (!isRoomMember(rp, userId)) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    /**
     * @return 错误信息；成功返回 empty
     */
    public Optional<String> validateAndInsert(
            GameRoom room, int senderColor, long senderUserId, JsonNode root, RoomChatMessage outRow) {
        String kind = root.path("kind").asText("").trim().toUpperCase(Locale.ROOT);
        String roomId = room.getRoomId();
        RoomParticipant rp = roomParticipantMapper.selectByRoomId(roomId);
        if (rp == null) {
            return Optional.of("房间不存在");
        }
        if (!isRoomPlayer(rp, senderUserId)) {
            return Optional.of("无权发言");
        }
        Optional<Integer> seat = resolveSenderColor(rp, senderUserId);
        if (!seat.isPresent() || seat.get() != senderColor) {
            return Optional.of("座位与消息不匹配");
        }

        String content;
        if (KIND_TEXT.equals(kind)) {
            content = root.path("text").asText("");
            content = normalizeUserText(content);
            if (content.isEmpty()) {
                return Optional.of("内容为空");
            }
            if (codePointCount(content) > MAX_TEXT_CODEPOINTS) {
                return Optional.of("最多30字");
            }
            content =
                    ChatAbusiveTextFilter.maskAbusiveText(
                            ChatSensitiveInfoFilter.maskSensitiveInfo(content));
            Optional<String> wxRisk =
                    weChatMsgSecCheckService.rejectIfWeChatRisky(senderUserId, content);
            if (wxRisk.isPresent()) {
                return wxRisk;
            }
        } else if (KIND_QUICK.equals(kind)) {
            content = root.path("text").asText("");
            content = normalizeUserText(content);
            if (!QUICK_PHRASES.contains(content)) {
                return Optional.of("无效快捷用语");
            }
        } else if (KIND_EMOJI.equals(kind)) {
            content = root.path("text").asText("");
            if (!EMOJIS.contains(content)) {
                return Optional.of("无效表情");
            }
        } else {
            return Optional.of("未知 kind");
        }

        outRow.setRoomId(roomId);
        outRow.setSenderUserId(senderUserId);
        outRow.setKind(kind);
        outRow.setContent(content);
        outRow.setId(nextEphemeralMessageId());
        outRow.setCreatedAt(new Date());
        return Optional.empty();
    }

    public ObjectNode toChatBroadcastJson(RoomChatMessage row, int senderColor) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("type", "CHAT");
        n.put("id", row.getId());
        n.put("roomId", row.getRoomId());
        n.put("senderUserId", row.getSenderUserId());
        n.put("senderColor", senderColor);
        n.put("kind", row.getKind());
        n.put("text", row.getContent());
        long ts =
                row.getCreatedAt() != null
                        ? row.getCreatedAt().getTime()
                        : System.currentTimeMillis();
        n.put("createdAt", ts);
        return n;
    }

    private static boolean isRoomMember(RoomParticipant rp, long uid) {
        if (uid == rp.getBlackUserId()) {
            return true;
        }
        if (rp.getWhiteUserId() != null && uid == rp.getWhiteUserId()) {
            return true;
        }
        return rp.getObserverUserId() != null && uid == rp.getObserverUserId();
    }

    /** 棋手（不含观战） */
    private static boolean isRoomPlayer(RoomParticipant rp, long uid) {
        if (uid == rp.getBlackUserId()) {
            return true;
        }
        return rp.getWhiteUserId() != null && uid == rp.getWhiteUserId();
    }

    private static Optional<Integer> resolveSenderColor(RoomParticipant rp, long senderUserId) {
        if (senderUserId == rp.getBlackUserId()) {
            return Optional.of(Stone.BLACK);
        }
        if (rp.getWhiteUserId() != null && senderUserId == rp.getWhiteUserId()) {
            return Optional.of(Stone.WHITE);
        }
        return Optional.empty();
    }

    private static String normalizeUserText(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        int i = 0;
        int n = t.length();
        StringBuilder sb = new StringBuilder(n);
        while (i < n) {
            int cp = t.codePointAt(i);
            if (!Character.isISOControl(cp)) {
                sb.appendCodePoint(cp);
            }
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    private static int codePointCount(String s) {
        int c = 0;
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            c++;
            i += Character.charCount(cp);
        }
        return c;
    }

    private static String trim(String s, int maxChars) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.length() <= maxChars) {
            return t;
        }
        return t.substring(0, maxChars);
    }
}
