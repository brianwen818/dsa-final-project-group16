package com.group16.ooosearch.search;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;

/**
 * 把使用者的查詢限定在《假面騎士OOO》的範圍內（Stage 3）。
 *
 * <p>使用者可能輸入和 OOO 無關的詞（例如「貓咪」）。直接拿去搜尋只會找到一般講貓的網頁，
 * 所以送給外部搜尋來源前，先在查詢後面加上主題名稱「假面騎士OOO」；查詢本身已經提到 OOO 時不再加。
 * 查詢詞的其他寫法與排除詞（例如「貓咪」的「貓」「猫」，以及要排除的「熊貓」）來自 {@code data/query-synonyms.txt}。
 */
public final class QueryScope {
    /** 加在查詢後面的主題名稱。 */
    public static final String TOPIC = "假面騎士OOO";

    private QueryScope() {
    }

    /**
     * 把查詢包成一個 Keyword：名稱是查詢本身，其他寫法來自同義詞表。
     * 同義詞表每行格式為「寫法1 | 寫法2, 寫法3, ... | !排除1, !排除2」，查詢等於該行任一寫法（不分大小寫）時，整行都算進來。
     * 檔案不存在時只用查詢本身。
     */
    public static Keyword queryKeyword(String query, Path synonymsFile) throws IOException {
        return queryKeyword(query, synonymsFile, new KeywordList());
    }

    /**
     * 同上；查詢剛好是 OOO 關鍵字的某種寫法時（例如「核心硬幣」），也沿用關鍵字表中的所有寫法
     * （「核心硬币」「コアメダル」），否則簡體或日文網頁會被當成沒提到查詢詞。
     */
    public static Keyword queryKeyword(String query, Path synonymsFile, KeywordList keywords) throws IOException {
        String q = query.strip();
        Set<String> aliases = new LinkedHashSet<>();
        for (Keyword keyword : keywords) {
            if (keyword.getWeight() > 0 && keyword.getForms().stream().anyMatch(f -> f.equalsIgnoreCase(q))) {
                keyword.getForms().stream().filter(f -> !f.equalsIgnoreCase(q)).forEach(aliases::add);
            }
        }
        for (List<String> line : matchingLines(q, synonymsFile)) {
            line.stream().filter(f -> !isExclusion(f) && !f.equalsIgnoreCase(q)).forEach(aliases::add);
        }
        return new Keyword(q, 1.0, new ArrayList<>(aliases));
    }

    /** 查詢詞自己的排除詞（同義詞表中以 ! 開頭的寫法），權重為 0。 */
    public static List<Keyword> queryExclusions(String query, Path synonymsFile) throws IOException {
        Set<String> names = new LinkedHashSet<>();
        for (List<String> line : matchingLines(query.strip(), synonymsFile)) {
            line.stream().filter(QueryScope::isExclusion).map(f -> f.substring(1).strip()).forEach(names::add);
        }
        List<Keyword> result = new ArrayList<>();
        for (String name : names) {
            if (!name.isEmpty()) {
                result.add(new Keyword(name, 0));
            }
        }
        return result;
    }

    /**
     * 送給外部搜尋來源的查詢字串：取查詢的前 maxVariants 個寫法，各自加上主題名稱。
     * 寫法本身已含主題的任一寫法（例如「OOO 結局」）時，原樣送出。
     */
    public static List<String> scopedQueries(Keyword query, Keyword topic, int maxVariants) {
        Set<String> result = new LinkedHashSet<>();
        for (String form : query.getForms()) {
            if (result.size() >= maxVariants) {
                break;
            }
            result.add(scoped(form, topic));
        }
        return new ArrayList<>(result);
    }

    /** 單一詞加上主題名稱；已提到主題時原樣回傳。 */
    public static String scoped(String term, Keyword topic) {
        return mentionsTopic(term, topic) ? term : term + " " + TOPIC;
    }

    /** 查詢是否已經提到主題（不分大小寫比對主題的所有寫法）。 */
    public static boolean mentionsTopic(String text, Keyword topic) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String form : topic.getForms()) {
            if (lower.contains(form.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /** 關鍵字表中的主題關鍵字（假面騎士OOO）；找不到時用第一個關鍵字。 */
    public static Keyword findTopic(KeywordList keywords) {
        for (Keyword keyword : keywords) {
            if (keyword.getName().equals(TOPIC)) {
                return keyword;
            }
        }
        return keywords.get(0);
    }

    private static boolean isExclusion(String form) {
        return form.startsWith("!");
    }

    /** 同義詞表中，任一個非排除寫法等於查詢的行（每行拆成寫法清單）。 */
    private static List<List<String>> matchingLines(String q, Path synonymsFile) throws IOException {
        List<List<String>> result = new ArrayList<>();
        if (synonymsFile == null || !Files.exists(synonymsFile)) {
            return result;
        }
        for (String line : Files.readAllLines(synonymsFile, StandardCharsets.UTF_8)) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            List<String> forms = KeywordList.splitForms(line.replace('|', ','));
            if (forms.stream().anyMatch(f -> !isExclusion(f) && f.equalsIgnoreCase(q))) {
                result.add(forms);
            }
        }
        return result;
    }
}
