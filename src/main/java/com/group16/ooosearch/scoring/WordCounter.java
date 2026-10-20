package com.group16.ooosearch.scoring;

import java.util.LinkedHashMap;
import java.util.Map;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;

/**
 * 計算每個關鍵字（含所有寫法）在文字中出現的次數（由 HW3 的 WordCounter 延伸）。
 *
 * 規則：
 * 1. 文字與寫法都先經過 TextNormalizer.forMatching（全形轉半形、忽略大小寫）。
 * 2. 長的寫法先比對，比對到的位置用 boolean[] 標記；之後較短的寫法不能使用已標記的位置。
 *    因此「假面騎士OOO」不會再被算成一次「OOO」，排除詞「安卡拉」也不會被算成「安卡」。
 * 3. 寫法開頭（結尾）是英文字母或數字時，前一個（後一個）字元不能也是英文字母或數字，
 *    避免「Birth」比對到「Birthday」。
 */
public class WordCounter {
    private final KeywordList keywords;
    private final SynonymTable synonyms;

    public WordCounter(KeywordList keywords) {
        this.keywords = keywords;
        this.synonyms = new SynonymTable(keywords);
    }

    /** 回傳「關鍵字名稱 → 次數」，依關鍵字的順序，包含次數為 0 的關鍵字。 */
    public Map<String, Integer> count(String text) {
        String t = TextNormalizer.forMatching(text);
        boolean[] used = new boolean[t.length()];

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Keyword keyword : keywords) {
            counts.put(keyword.getName(), 0);
        }
        for (String form : synonyms.formsLongestFirst()) {
            String keyword = synonyms.keywordOf(form);
            for (int i = t.indexOf(form); i >= 0; i = t.indexOf(form, i + 1)) {
                int end = i + form.length();
                if (isUsed(used, i, end) || !hasWordBoundary(t, form, i, end)) {
                    continue;
                }
                for (int j = i; j < end; j++) {
                    used[j] = true;
                }
                counts.merge(keyword, 1, Integer::sum);
            }
        }
        return counts;
    }

    private static boolean isUsed(boolean[] used, int start, int end) {
        for (int i = start; i < end; i++) {
            if (used[i]) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasWordBoundary(String text, String form, int start, int end) {
        if (isAsciiAlphanumeric(form.charAt(0)) && start > 0 && isAsciiAlphanumeric(text.charAt(start - 1))) {
            return false;
        }
        return !(isAsciiAlphanumeric(form.charAt(form.length() - 1))
                && end < text.length() && isAsciiAlphanumeric(text.charAt(end)));
    }

    private static boolean isAsciiAlphanumeric(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }
}
