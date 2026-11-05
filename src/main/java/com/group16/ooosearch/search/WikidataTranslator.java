package com.group16.ooosearch.search;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.group16.ooosearch.fetch.PageFetcher;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 用 Wikidata 把外語查詢翻成中文、日文寫法（免費、不需要 API 金鑰）。
 *
 * <p>Wikidata 的每個概念都有各語言的名稱，例如「貓」（Q146）有中文「貓」、日文「ネコ」、法文「chat」。
 * 不需要使用者指定輸入語言，只依文字系統決定要查哪些語言：
 * <ul>
 *   <li>含漢字或假名：已經是中日文，不翻譯。</li>
 *   <li>韓文、俄文等：由字母決定語言。</li>
 *   <li>拉丁字母：分不出是英、法、德文…，所以幾種常見語言都查。</li>
 * </ul>
 * 同一個詞可能有好幾個意思（法文 chat 是貓，英文 chat 是聊天），所以取前幾個概念的名稱都當別名，
 * 由「OOO 分數 × 查詢詞相關度」自然過濾：和 OOO 不會一起出現的意思分數就低。
 *
 * <p>所有請求都透過 PageFetcher，因此會使用快取並控制連線頻率；同一個詞只會查一次。
 */
public class WikidataTranslator implements QueryTranslator {
    static final String API = "https://www.wikidata.org/w/api.php";
    /** 拉丁字母查詢要查的語言（依序）。 */
    static final List<String> LATIN_LANGUAGES = List.of("en", "fr", "de", "es", "it", "pt");
    /** 取名稱的語言，依偏好順序（繁中優先）。 */
    static final List<String> TARGET_LANGUAGES = List.of("zh-tw", "zh-hant", "zh-hk", "zh", "zh-hans", "zh-cn", "ja");
    /** 每種語言取前幾個概念。 */
    static final int RESULTS_PER_LANGUAGE = 2;
    /** 最多用幾個概念的名稱。 */
    static final int MAX_ENTITIES = 3;

    private static final Pattern CJK = Pattern.compile("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}]");

    private final PageFetcher fetcher;
    private final JsonMapper json = JsonMapper.builder().build();

    public WikidataTranslator(PageFetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public List<String> translate(String query) throws IOException {
        String q = query.strip();
        if (q.isEmpty() || containsCjk(q)) {
            return List.of();
        }
        List<String> ids = searchEntities(q, languagesFor(q));
        if (ids.isEmpty()) {
            return List.of();
        }
        return labels(ids, q);
    }

    /** 文字是否含漢字或假名。 */
    public static boolean containsCjk(String text) {
        return CJK.matcher(text).find();
    }

    /** 依文字系統決定要查哪些語言；拉丁字母與無法判斷的文字查 {@link #LATIN_LANGUAGES}。 */
    static List<String> languagesFor(String query) {
        for (int i = 0; i < query.length(); ) {
            int cp = query.codePointAt(i);
            i += Character.charCount(cp);
            switch (Character.UnicodeScript.of(cp)) {
                case HANGUL: return List.of("ko");
                case CYRILLIC: return List.of("ru");
                case GREEK: return List.of("el");
                case ARABIC: return List.of("ar");
                case HEBREW: return List.of("he");
                case THAI: return List.of("th");
                default: break;
            }
        }
        return LATIN_LANGUAGES;
    }

    static String searchUrl(String query, String lang) {
        return API + "?action=wbsearchentities&format=json&type=item&limit=" + RESULTS_PER_LANGUAGE
                + "&language=" + lang + "&uselang=" + lang + "&search=" + encode(query);
    }

    static String entitiesUrl(List<String> ids) {
        return API + "?action=wbgetentities&format=json&props=labels"
                + "&languages=" + encode(String.join("|", TARGET_LANGUAGES))
                + "&ids=" + encode(String.join("|", ids));
    }

    /** 候選概念的命中紀錄：最佳名次、被幾種語言查到、第一次被哪種語言查到。 */
    private static final class Hit {
        int bestRank = Integer.MAX_VALUE;
        int languages = 0;
        int firstLanguage = Integer.MAX_VALUE;
    }

    /**
     * 每種語言各查一次，合併候選概念：最佳名次小的優先，同名次時被越多語言查到的優先，再依語言順序。
     * 最多回傳 {@link #MAX_ENTITIES} 個。
     */
    private List<String> searchEntities(String query, List<String> languages) throws IOException {
        Map<String, Hit> hits = new LinkedHashMap<>();
        for (int l = 0; l < languages.size(); l++) {
            JsonNode results = request(searchUrl(query, languages.get(l))).path("search");
            int rank = 0;
            for (JsonNode result : results) {
                String id = result.path("id").asString();
                if (id.isEmpty()) {
                    continue;
                }
                Hit hit = hits.computeIfAbsent(id, k -> new Hit());
                hit.bestRank = Math.min(hit.bestRank, rank);
                hit.languages++;
                hit.firstLanguage = Math.min(hit.firstLanguage, l);
                rank++;
            }
        }
        List<Map.Entry<String, Hit>> sorted = new ArrayList<>(hits.entrySet());
        sorted.sort(Comparator.<Map.Entry<String, Hit>>comparingInt(e -> e.getValue().bestRank)
                .thenComparingInt(e -> -e.getValue().languages)
                .thenComparingInt(e -> e.getValue().firstLanguage));
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < sorted.size() && i < MAX_ENTITIES; i++) {
            ids.add(sorted.get(i).getKey());
        }
        return ids;
    }

    /** 依概念順序、語言偏好順序收集中日文名稱；只保留含漢字或假名的名稱（濾掉沒有中日文名稱的概念）。 */
    private List<String> labels(List<String> ids, String query) throws IOException {
        JsonNode entities = request(entitiesUrl(ids)).path("entities");
        Set<String> result = new LinkedHashSet<>();
        for (String id : ids) {
            JsonNode labels = entities.path(id).path("labels");
            for (String lang : TARGET_LANGUAGES) {
                String label = labels.path(lang).path("value").asString("").strip();
                if (!label.isEmpty() && containsCjk(label) && !label.equalsIgnoreCase(query)) {
                    result.add(label);
                }
            }
        }
        return new ArrayList<>(result);
    }

    private JsonNode request(String url) throws IOException {
        JsonNode root = json.readTree(fetcher.fetchText(url));
        if (root.has("error")) {
            throw new IOException("Wikidata 查詢失敗：" + root.get("error").path("info").asString());
        }
        return root;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
