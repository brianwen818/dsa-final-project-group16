package com.group16.ooosearch.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.Passage;
import com.group16.ooosearch.tree.SiteResult;

/**
 * 一次搜尋的完整結果：使用的關鍵字、網站排名（含樹）、系統自動擴充的關鍵字，
 * 以及每個網站「為什麼相關」的段落摘要（2026-11-05 新增）。
 */
public class SearchResponse {
    private final String query;
    private final KeywordList keywords;
    private final List<SiteResult> results;
    private final List<Keyword> expandedKeywords;
    private final Map<String, List<Passage>> passages;

    public SearchResponse(String query, KeywordList keywords, List<SiteResult> results, List<Keyword> expandedKeywords) {
        this(query, keywords, results, expandedKeywords, Map.of());
    }

    /** passages 的 key 是網站的根網址（{@link SiteResult#getRootUrl()}）。 */
    public SearchResponse(String query, KeywordList keywords, List<SiteResult> results, List<Keyword> expandedKeywords,
                          Map<String, List<Passage>> passages) {
        this.query = query;
        this.keywords = keywords;
        this.results = Collections.unmodifiableList(results);
        this.expandedKeywords = Collections.unmodifiableList(expandedKeywords);
        this.passages = Collections.unmodifiableMap(new LinkedHashMap<>(passages));
    }

    public String getQuery() {
        return query;
    }

    /** 計分用的關鍵字（OOO 關鍵字 + 擴充詞）。 */
    public KeywordList getKeywords() {
        return keywords;
    }

    /** 依總分由高到低排序的網站；無法評分的網站排在最後。 */
    public List<SiteResult> getResults() {
        return results;
    }

    /** Stage 4 自動擴充出的關鍵字（不含原關鍵字）。 */
    public List<Keyword> getExpandedKeywords() {
        return expandedKeywords;
    }

    /** 某個網站的相關段落（最多 3 段，由高到低）；沒有時回傳空清單。 */
    public List<Passage> getPassages(String rootUrl) {
        return passages.getOrDefault(rootUrl, List.of());
    }

    /** 所有網站的相關段落：根網址 → 段落。 */
    public Map<String, List<Passage>> getAllPassages() {
        return passages;
    }
}
