package com.group16.ooosearch.snippet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.Passage;
import com.group16.ooosearch.model.WebPage;
import com.group16.ooosearch.scoring.QueryAwarePageScorer;
import com.group16.ooosearch.scoring.QueryCounter;
import com.group16.ooosearch.scoring.WordCounter;
import com.group16.ooosearch.search.QueryScope;
import com.group16.ooosearch.tree.SiteResult;
import com.group16.ooosearch.tree.WebNode;

/**
 * 從一個網站（整棵網站樹）挑出最能說明「為什麼和查詢有關」的段落（段落摘要的第 2、3 步）。
 *
 * <pre>
 * P_q(t) = ( α + ln(1 + q(t)) ) × ( 1 + Σ[k ∈ K] w(k) × c(k, t) )      α = 0.1
 * </pre>
 *
 * q(t) 是查詢詞在段落 t 的次數（q = 0 的段落不入選），c(k, t) 是 OOO 關鍵字的次數。
 * 入選資格：段落含查詢詞，而且（段落含原本的 OOO 關鍵字，或所在網頁的標題提到 OOO）。
 * 後者讓 OOO 條目中只寫「03 貓‧進化‧貪吃鬼」的集數列表也能入選；前者擋掉從子頁面爬到、和 OOO 無關的段落
 * （例如演員條目中的「旅貓日記」）。「原本的關鍵字」指權重 ≥ 2.6 的關鍵字，Stage 4 擴充詞（≤ 2.5）不算。
 * 用 PriorityQueue（大小 k 的最小堆積）從整棵樹的段落取前 k 名；開頭 40 字相同的段落視為重複，只留分數高的。
 */
public class PassageRanker {
    public static final int DEFAULT_TOP = 3;
    /** 摘要保留查詢詞前後各多少字。 */
    public static final int SNIPPET_RADIUS = 80;
    public static final int MAX_TAGS = 3;
    private static final int DEDUP_PREFIX = 40;
    /** 原本的 OOO 關鍵字權重至少 2.6；Stage 4 擴充詞最高 2.5（Proposal 2.4）。 */
    public static final double ORIGINAL_MIN_WEIGHT = 2.6;

    private final PassageExtractor extractor = new PassageExtractor();
    private final QueryCounter queryCounter;
    private final List<Keyword> queryExclusions;
    private final Keyword topic;
    private KeywordList cachedKeywords;
    private WordCounter cachedCounter;

    /** topic：主題關鍵字（假面騎士OOO），用來判斷網頁標題是否提到 OOO。 */
    public PassageRanker(Keyword query, List<Keyword> queryExclusions, Keyword topic) {
        this.queryCounter = new QueryCounter(query, queryExclusions);
        this.queryExclusions = List.copyOf(queryExclusions);
        this.topic = topic;
    }

    private record Candidate(PassageExtractor.Unit unit, WebPage page, double score, int q, Map<String, Integer> counts) {
    }

    /** 整棵網站樹中分數最高的 top 個段落，由高到低。 */
    public List<Passage> topPassages(SiteResult site, KeywordList keywords, int top) {
        return topPassages(site, keywords, top, new HashSet<>());
    }

    /**
     * 同上，但略過 alreadyShown 中已出現過的段落（以開頭 40 字比對），並把這次選出的段落加進去。
     * 排名較前的網站先選，後面的網站（例如網站樹重疊的維基條目）就會改顯示它自己獨有的段落。
     */
    public List<Passage> topPassages(SiteResult site, KeywordList keywords, int top, Set<String> alreadyShown) {
        Map<String, Candidate> best = new HashMap<>(); // 去重：開頭 40 字 → 分數最高的段落
        collect(site.getTree().getRoot(), keywords, best);
        best.keySet().removeAll(alreadyShown);

        PriorityQueue<Candidate> heap = new PriorityQueue<>(Comparator.comparingDouble(Candidate::score));
        for (Candidate c : best.values()) {
            heap.offer(c);
            if (heap.size() > top) {
                heap.poll(); // 丟掉目前最小的
            }
        }
        List<Passage> result = new ArrayList<>();
        while (!heap.isEmpty()) {
            Candidate c = heap.poll();
            alreadyShown.add(dedupKey(c.unit().text()));
            result.add(0, toPassage(c, keywords)); // 由大到小
        }
        return result;
    }

    /** 對單一網頁計算所有段落（測試與除錯用）。 */
    public List<Passage> rankPage(WebPage page, KeywordList keywords) {
        Map<String, Candidate> best = new HashMap<>();
        collectPage(page, keywords, best);
        List<Candidate> list = new ArrayList<>(best.values());
        list.sort(Comparator.comparingDouble(Candidate::score).reversed());
        List<Passage> result = new ArrayList<>();
        for (Candidate c : list) {
            result.add(toPassage(c, keywords));
        }
        return result;
    }

    private void collect(WebNode node, KeywordList keywords, Map<String, Candidate> best) {
        if (node.getPageResult() != null) {
            collectPage(node.getWebPage(), keywords, best);
        }
        for (WebNode child : node.getChildren()) {
            collect(child, keywords, best);
        }
    }

    private void collectPage(WebPage page, KeywordList keywords, Map<String, Candidate> best) {
        boolean pageAboutOoo = topic != null && QueryScope.mentionsTopic(page.getTitle(), topic);
        for (PassageExtractor.Unit unit : extractor.extract(page)) {
            int q = queryCounter.count(unit.text(), keywords);
            if (q == 0) {
                continue;
            }
            Map<String, Integer> counts = counterFor(keywords).count(unit.text());
            double ooo = 0;
            boolean mentionsOriginal = false;
            for (Keyword keyword : keywords) {
                int c = counts.get(keyword.getName());
                ooo += keyword.getWeight() * c;
                mentionsOriginal |= c > 0 && keyword.getWeight() >= ORIGINAL_MIN_WEIGHT;
            }
            if (!mentionsOriginal && !pageAboutOoo) {
                continue;
            }
            double score = (QueryAwarePageScorer.ALPHA + Math.log(1 + q)) * (1 + ooo);
            String key = dedupKey(unit.text());
            Candidate existing = best.get(key);
            if (existing == null || existing.score() < score) {
                best.put(key, new Candidate(unit, page, score, q, counts));
            }
        }
    }

    private Passage toPassage(Candidate c, KeywordList keywords) {
        PassageExtractor.Unit unit = c.unit();
        String anchor = unit.anchorId() == null ? c.page().getUrl() : c.page().getUrl() + "#" + unit.anchorId();
        String own = unit.ownImage(); // 段落自己的圖；章節圖只當備用（見 ImageFinder）
        return new Passage(snippet(unit.text()), unit.headingPath(), anchor, c.page().getUrl(), c.page().getTitle(),
                c.score(), c.q(), tags(c.counts(), unit.headingPath(), keywords),
                own, own == null ? null : c.page().getTitle(), own == null ? null : anchor, unit.sectionImage());
    }

    /** 標籤：段落中的 OOO 關鍵字，出現在章節標題中的排前面，其餘依 權重 × 次數 排序，最多 3 個。 */
    private static List<String> tags(Map<String, Integer> counts, List<String> headingPath, KeywordList keywords) {
        String headings = String.join(" ", headingPath).toLowerCase(Locale.ROOT);
        List<Keyword> present = new ArrayList<>();
        for (Keyword keyword : keywords) {
            if (keyword.getWeight() > 0 && counts.get(keyword.getName()) > 0) {
                present.add(keyword);
            }
        }
        present.sort(Comparator
                .comparing((Keyword k) -> !inHeadings(k, headings))
                .thenComparing(k -> -k.getWeight() * counts.get(k.getName())));
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_TAGS, present.size()); i++) {
            tags.add(present.get(i).getName());
        }
        return tags;
    }

    private static boolean inHeadings(Keyword keyword, String headings) {
        for (String form : keyword.getForms()) {
            if (headings.contains(form.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /** 以第一個（不是排除詞一部分的）查詢詞為中心，前後各留 SNIPPET_RADIUS 字。 */
    String snippet(String text) {
        int hit = firstQueryHit(text);
        if (text.length() <= 2 * SNIPPET_RADIUS || hit < 0) {
            return text.length() <= 2 * SNIPPET_RADIUS ? text : text.substring(0, 2 * SNIPPET_RADIUS) + "…";
        }
        int start = Math.max(0, hit - SNIPPET_RADIUS);
        int end = Math.min(text.length(), hit + SNIPPET_RADIUS);
        return (start > 0 ? "…" : "") + text.substring(start, end) + (end < text.length() ? "…" : "");
    }

    private int firstQueryHit(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        int best = -1;
        for (String form : queryCounter.getQuery().getForms()) {
            String f = form.toLowerCase(Locale.ROOT);
            for (int i = lower.indexOf(f); i >= 0; i = lower.indexOf(f, i + 1)) {
                if (!insideExclusion(lower, i, f)) {
                    if (best < 0 || i < best) {
                        best = i;
                    }
                    break;
                }
            }
        }
        return best;
    }

    /** 位置 i 的查詢詞是否其實是某個排除詞（例如「熊貓」中的「貓」）的一部分。 */
    private boolean insideExclusion(String lower, int i, String form) {
        for (Keyword exclusion : queryExclusions) {
            for (String e : exclusion.getForms()) {
                String el = e.toLowerCase(Locale.ROOT);
                int offset = el.indexOf(form);
                if (offset >= 0 && i - offset >= 0 && lower.startsWith(el, i - offset)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String dedupKey(String text) {
        String normalized = text.replaceAll("\\s+", "");
        return normalized.substring(0, Math.min(DEDUP_PREFIX, normalized.length()));
    }

    private WordCounter counterFor(KeywordList keywords) {
        if (keywords != cachedKeywords || cachedCounter == null) {
            cachedKeywords = keywords;
            cachedCounter = new WordCounter(keywords);
        }
        return cachedCounter;
    }
}
