package com.gomoku.sync.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对局聊天文本脱敏：手机、邮箱、链接、证件号片段、连续 8 位及以上数字等替换为 {@code *}（等长），与小程序端 {@code maskChatTextSensitive} 一致。
 */
public final class ChatSensitiveInfoFilter {

    private static final Pattern EMAIL =
            Pattern.compile(
                    "(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}");
    private static final Pattern URL_HTTP = Pattern.compile("(?i)https?://\\S*");
    /** www. 起非空白片段，避免吞掉整句；与 JS 端一致 */
    private static final Pattern URL_WWW = Pattern.compile("(?i)\\bwww\\.\\S+");
    private static final Pattern CN_ID_18 = Pattern.compile("\\d{17}[0-9Xx]");

    private ChatSensitiveInfoFilter() {}

    /**
     * @param userText 已 trim、去控制符后的用户输入
     * @return 脱敏后的文本（NFKC 规范化后再打码，可能与原文个别全角字符形式不同）
     */
    public static String maskSensitiveInfo(String userText) {
        if (userText == null || userText.isEmpty()) {
            return userText;
        }
        String n = Normalizer.normalize(userText, Normalizer.Form.NFKC);
        List<int[]> raw = new ArrayList<>();
        addPatternIntervals(n, EMAIL, raw);
        addPatternIntervals(n, URL_HTTP, raw);
        addPatternIntervals(n, URL_WWW, raw);
        addPatternIntervals(n, CN_ID_18, raw);
        addPhoneIntervals(n, raw);
        addLongAsciiDigitRuns(n, raw);
        List<int[]> merged = mergeIntervals(raw);
        return applyAsteriskMask(n, merged);
    }

    private static void addPatternIntervals(String s, Pattern p, List<int[]> raw) {
        Matcher m = p.matcher(s);
        while (m.find()) {
            raw.add(new int[] {m.start(), m.end()});
        }
    }

    private static void addPhoneIntervals(String n, List<int[]> raw) {
        List<Integer> starts = new ArrayList<>();
        int i = 0;
        while (i < n.length()) {
            int cp = n.codePointAt(i);
            if (cp >= '0' && cp <= '9') {
                starts.add(i);
                i += Character.charCount(cp);
            } else {
                i += Character.charCount(cp);
            }
        }
        int m = starts.size();
        for (int j = 0; j + 11 <= m; j++) {
            StringBuilder sb = new StringBuilder(11);
            for (int k = j; k < j + 11; k++) {
                sb.appendCodePoint(n.codePointAt(starts.get(k)));
            }
            if (sb.toString().matches("1[3-9]\\d{9}")) {
                int a = starts.get(j);
                int lastStart = starts.get(j + 10);
                int end = lastStart + Character.charCount(n.codePointAt(lastStart));
                raw.add(new int[] {a, end});
            }
        }
    }

    /** NFKC 后连续 ASCII 数字段长度 ≥8（与 JS 端一致） */
    private static void addLongAsciiDigitRuns(String n, List<int[]> raw) {
        int i = 0;
        while (i < n.length()) {
            int cp = n.codePointAt(i);
            int w = Character.charCount(cp);
            if (cp >= '0' && cp <= '9') {
                int start = i;
                i += w;
                while (i < n.length()) {
                    cp = n.codePointAt(i);
                    if (cp < '0' || cp > '9') {
                        break;
                    }
                    w = Character.charCount(cp);
                    i += w;
                }
                if (i - start >= 8) {
                    raw.add(new int[] {start, i});
                }
            } else {
                i += w;
            }
        }
    }

    private static List<int[]> mergeIntervals(List<int[]> raw) {
        if (raw.isEmpty()) {
            return raw;
        }
        raw.sort(Comparator.comparingInt(a -> a[0]));
        List<int[]> out = new ArrayList<>();
        int cs = raw.get(0)[0];
        int ce = raw.get(0)[1];
        for (int k = 1; k < raw.size(); k++) {
            int s = raw.get(k)[0];
            int e = raw.get(k)[1];
            if (s <= ce) {
                ce = Math.max(ce, e);
            } else {
                out.add(new int[] {cs, ce});
                cs = s;
                ce = e;
            }
        }
        out.add(new int[] {cs, ce});
        return out;
    }

    private static String applyAsteriskMask(String s, List<int[]> merged) {
        if (merged.isEmpty()) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        int cur = 0;
        for (int[] range : merged) {
            int a = range[0];
            int b = range[1];
            sb.append(s, cur, a);
            for (int z = a; z < b; z++) {
                sb.append('*');
            }
            cur = b;
        }
        sb.append(s.substring(cur));
        return sb.toString();
    }
}
