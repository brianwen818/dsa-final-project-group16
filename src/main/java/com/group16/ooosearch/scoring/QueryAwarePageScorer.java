package com.group16.ooosearch.scoring;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.group16.ooosearch.fetch.TextExtractor;
import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.PageResult;
import com.group16.ooosearch.model.WebPage;

/**
 * 同時考慮「和 OOO 的相關程度」與「和查詢詞的相關程度」的單頁計分（Stage 3）。
 *
 * <pre>
 * S_q(p) = S(p) × ( α + ln(1 + q(p)) )
 * </pre>
 *
 * S(p) 是 Stage 1 的 OOO 分數（Σ 權重 × 次數），q(p) 是查詢詞（含其他寫法）在正文出現的次數，α = 0.1。
 * <ul>
 *   <li>沒提到 OOO 的頁面：S(p) = 0，總分為 0，例如一般講貓的網頁。</li>
 *   <li>沒提到查詢詞的 OOO 頁面：只保留 10% 的分數；不設為 0，是為了讓爬蟲仍會展開它的子頁面。</li>
 *   <li>兩者都有的頁面：查詢詞出現越多分數越高，但取 ln，避免一個詞重複很多次就無限放大。</li>
 * </ul>
 * 查詢詞的計數規則見 {@link QueryCounter}。
 */
public class QueryAwarePageScorer implements PageScorer {
    /** 沒提到查詢詞時保留的分數比例。 */
    public static final double ALPHA = 0.1;

    private final QueryCounter queryCounter;
    private final KeywordPageScorer base = new KeywordPageScorer();
    private final TextExtractor extractor = new TextExtractor();

    public QueryAwarePageScorer(Keyword query) {
        this(query, List.of());
    }

    /** queryExclusions：查詢詞自己的排除詞（例如查「貓」時的「熊貓」）。 */
    public QueryAwarePageScorer(Keyword query, List<Keyword> queryExclusions) {
        this.queryCounter = new QueryCounter(query, queryExclusions);
    }

    /** PageResult 的關鍵字次數表中，查詢詞的名稱（加上前綴，避免和 OOO 關鍵字同名時互相覆蓋）。 */
    public String queryLabel() {
        return "查詢:" + queryCounter.getQuery().getName();
    }

    @Override
    public PageResult score(WebPage page, KeywordList keywords) {
        PageResult oooResult = base.score(page, keywords);
        int q = queryCounter.count(extractor.extract(page.getHtml()), keywords);
        double factor = ALPHA + Math.log(1 + q);

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put(queryLabel(), q);
        counts.putAll(oooResult.getKeywordCounts());
        return new PageResult(page, oooResult.getScore() * factor, counts);
    }
}
