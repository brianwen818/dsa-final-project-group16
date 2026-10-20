package com.group16.ooosearch.scoring;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 比對關鍵字前的文字正規化，讓不同寫法的同一個詞能比對得到。
 */
public final class TextNormalizer {
    private static final Pattern CJK_SPACE = Pattern.compile(
            "(?<=[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}ー])\\s+(?=[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}ー])");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private TextNormalizer() {
    }

    /**
     * 全形轉半形（ＯＯＯ → OOO、（ → (）、移除漢字與假名之間的空白（「火野 映司」→「火野映司」）、
     * 合併多餘空白。保留大小寫。
     */
    public static String normalize(String text) {
        String n = Normalizer.normalize(text, Normalizer.Form.NFKC);
        n = CJK_SPACE.matcher(n).replaceAll("");
        return WHITESPACE.matcher(n).replaceAll(" ").strip();
    }

    /** normalize 之後再轉小寫，用於忽略大小寫的比對。 */
    public static String forMatching(String text) {
        return normalize(text).toLowerCase(Locale.ROOT);
    }
}
