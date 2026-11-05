package com.group16.ooosearch.search;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.group16.ooosearch.model.Keyword;

/**
 * 找候選網址（Stage 3）：
 * <ul>
 *   <li>Google 快取用原始查詢，因為它同時是對照組（組員手動記錄的就是原始查詢的結果）。</li>
 *   <li>其他來源（維基、DuckDuckGo）用「查詢 + 假面騎士OOO」，每個查詢最多取 2 種寫法。</li>
 * </ul>
 * 所有網址先正規化，再用 LinkedHashSet 去除重複並保留順序。
 */
public class CandidateFinder {
    public static final int LIMIT_PER_PROVIDER = 5;
    public static final int MAX_QUERY_VARIANTS = 2;

    private final SearchProvider google;
    private final SearchAggregator scopedAggregator;

    public CandidateFinder(SearchProvider google, List<SearchProvider> scopedProviders) {
        this.google = google;
        this.scopedAggregator = new SearchAggregator(scopedProviders);
    }

    /** 第一輪候選網址：Google 前幾筆，加上各寫法的 scoped 查詢結果。 */
    public List<String> find(String query, Keyword queryKeyword, Keyword topic) {
        Set<String> candidates = new LinkedHashSet<>(
                new SearchAggregator(List.of(google)).search(query, LIMIT_PER_PROVIDER));
        for (String scoped : QueryScope.scopedQueries(queryKeyword, topic, MAX_QUERY_VARIANTS)) {
            candidates.addAll(scopedAggregator.search(scoped, LIMIT_PER_PROVIDER));
        }
        return new ArrayList<>(candidates);
    }

    /** 擴充後的再搜尋（Stage 4）：每個擴充詞加上主題名稱，只用維基與 DuckDuckGo。 */
    public List<String> findMore(List<String> terms, Keyword topic, int limitPerProvider) {
        Set<String> candidates = new LinkedHashSet<>();
        for (String term : terms) {
            candidates.addAll(scopedAggregator.search(QueryScope.scoped(term, topic), limitPerProvider));
        }
        return new ArrayList<>(candidates);
    }
}
