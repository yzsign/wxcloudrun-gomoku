package com.gomoku.sync.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对局聊天：辱骂、攻击性用语打码（等长 *）。与小程序 {@code maskChatTextAbusive} 词表与顺序需保持一致。
 * 先 NFKC，再按词长降序替换中文片段，最后替换英文辱骂词（词边界）。
 */
public final class ChatAbusiveTextFilter {

    /**
     * 中文辱骂/攻击性短语；运行时会按 UTF-16 长度降序排序，优先匹配长词。
     */
    private static final String[] CN_TERMS_RAW = {
        "motherfucker",
        "操你妈",
        "草泥马",
        "王八蛋",
        "狗杂种",
        "狗日的",
        "狗东西",
        "杀了你",
        "砍死你",
        "下三滥",
        "神经病",
        "去死吧",
        "去死吗",
        "滚远点",
        "滚一边",
        "尼玛的",
        "尼玛逼",
        "你麻痹",
        "泥马逼",
        "贱骨头",
        "臭婊子",
        "臭傻逼",
        "死全家",
        "没爹妈",
        "没娘养",
        "杂种东西",
        "畜生东西",
        "人渣废物",
        "白痴废物",
        "弱智儿童",
        "脑残玩意",
        "沙雕玩意",
        "蠢货玩意",
        "贱人玩意",
        "烂人一个",
        "骚货玩意",
        "尼玛",
        "泥马",
        "傻逼",
        "傻b",
        "傻B",
        "萨比",
        "沙比",
        "蠢猪",
        "白痴",
        "弱智",
        "智障",
        "脑残",
        "沙雕",
        "废物",
        "人渣",
        "贱人",
        "贱货",
        "烂人",
        "婊子",
        "骚货",
        "贱婢",
        "混蛋",
        "混账",
        "畜生",
        "杂种",
        "下贱",
        "滚蛋",
        "滚开",
        "滚粗",
        "滚远",
        "nmsl",
        "NMSL",
        "cnm",
        "CNM",
        "sb东西",
        "艹你",
        "日你",
    };

    private static final String[] CN_TERMS_SORTED;

    static {
        CN_TERMS_SORTED = Arrays.copyOf(CN_TERMS_RAW, CN_TERMS_RAW.length);
        Arrays.sort(CN_TERMS_SORTED, (a, b) -> Integer.compare(b.length(), a.length()));
    }

    /** 英文辱骂词（词边界），不含种族仇恨类词汇 */
    private static final Pattern EN_ABUSE =
            Pattern.compile(
                    "\\b(fuck|fucks|fucked|fucking|fucker|shit|shits|bitch|bitches|bitching|asshole|"
                            + "bastard|bastards|slut|sluts|whore|whores|damn|piss|"
                            + "motherfuckers?|dickhead|dickheads|douche|douchebag|crap|bullshit|"
                            + "assholes?|cunts?|pricks?|wanker)\\b",
                    Pattern.CASE_INSENSITIVE);

    private ChatAbusiveTextFilter() {}

    public static String maskAbusiveText(String userText) {
        if (userText == null || userText.isEmpty()) {
            return userText;
        }
        String n = Normalizer.normalize(userText, Normalizer.Form.NFKC);
        String out = maskChineseTerms(n);
        return maskEnglishTerms(out);
    }

    private static String maskChineseTerms(String s) {
        String out = s;
        for (String term : CN_TERMS_SORTED) {
            if (term.isEmpty()) {
                continue;
            }
            if (!out.contains(term)) {
                continue;
            }
            String stars = starsUtf16(term.length());
            out = out.replace(term, stars);
        }
        return out;
    }

    private static String maskEnglishTerms(String out) {
        Matcher m = EN_ABUSE.matcher(out);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            int len = m.end() - m.start();
            m.appendReplacement(sb, Matcher.quoteReplacement(starsUtf16(len)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String starsUtf16(int utf16Length) {
        if (utf16Length <= 0) {
            return "";
        }
        char[] c = new char[utf16Length];
        Arrays.fill(c, '*');
        return new String(c);
    }
}
