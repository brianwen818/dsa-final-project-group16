package com.group16.ooosearch.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 單一網頁的計分結果（Stage 1 的輸出）：分數與各關鍵字出現次數。
 */
public class PageResult {
    private final WebPage page;
    private final double score;
    private final Map<String, Integer> keywordCounts;

    /** keywordCounts 的 key 是關鍵字名稱，順序會被保留。 */
    public PageResult(WebPage page, double score, Map<String, Integer> keywordCounts) {
        this.page = page;
        this.score = score;
        this.keywordCounts = Collections.unmodifiableMap(new LinkedHashMap<>(keywordCounts));
    }

    public WebPage getPage() {
        return page;
    }

    public double getScore() {
        return score;
    }

    public Map<String, Integer> getKeywordCounts() {
        return keywordCounts;
    }

    /** 回傳某個關鍵字的出現次數；沒有統計到的關鍵字回傳 0。 */
    public int getCount(String keywordName) {
        return keywordCounts.getOrDefault(keywordName, 0);
    }
}
