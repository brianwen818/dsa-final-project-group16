package com.group16.ooosearch.engine;

import java.util.Collections;
import java.util.List;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.tree.SiteResult;

/**
 * 一次搜尋的完整結果：使用的關鍵字、網站排名（含樹）、系統自動擴充的關鍵字。
 */
public class SearchResponse {
    private final String query;
    private final KeywordList keywords;
    private final List<SiteResult> results;
    private final List<Keyword> expandedKeywords;

    public SearchResponse(String query, KeywordList keywords, List<SiteResult> results, List<Keyword> expandedKeywords) {
        this.query = query;
        this.keywords = keywords;
        this.results = Collections.unmodifiableList(results);
        this.expandedKeywords = Collections.unmodifiableList(expandedKeywords);
    }

    public String getQuery() {
        return query;
    }

    /** 使用者查詢轉成的關鍵字。 */
    public KeywordList getKeywords() {
        return keywords;
    }

    /** 依總分由高到低排序的網站。 */
    public List<SiteResult> getResults() {
        return results;
    }

    /** Stage 4 自動擴充出的關鍵字（不含原關鍵字）。 */
    public List<Keyword> getExpandedKeywords() {
        return expandedKeywords;
    }
}
