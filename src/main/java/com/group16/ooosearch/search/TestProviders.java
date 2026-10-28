package com.group16.ooosearch.search;

import com.group16.ooosearch.fetch.PageCache;
import com.group16.ooosearch.fetch.PageFetcher;

import java.util.List;

public class TestProviders {
    public static void main(String[] args) {
        PageCache cache = new PageCache();
        PageFetcher fetcher = new PageFetcher(cache);

        SearchProvider google = new CachedGoogleProvider();
        SearchProvider wiki = new MediaWikiProvider(fetcher);
        SearchProvider ddg = new DuckDuckGoProvider(fetcher);

        String query = "假面騎士OOO";

        System.out.println("=== 測試 CachedGoogleProvider ===");
        List<String> googleResults = google.search(query, 5);
        googleResults.forEach(System.out::println);

        System.out.println("\n=== 測試 MediaWikiProvider ===");
        List<String> wikiResults = wiki.search(query, 5);
        wikiResults.forEach(System.out::println);

        System.out.println("\n=== 測試 DuckDuckGoProvider ===");
        List<String> ddgResults = ddg.search(query, 5);
        ddgResults.forEach(System.out::println);

        // =========================================================
        // 驗證 SearchAggregator 整合與去重邏輯
        // =========================================================
        System.out.println("\n=== 測試 SearchAggregator (整合與去重) ===");
        List<SearchProvider> providers = List.of(google, wiki, ddg);
        SearchAggregator aggregator = new SearchAggregator(providers);

        List<String> combinedResults = aggregator.search(query, 5);
        System.out.println("整合去重後的最終搜尋結果 (共 " + combinedResults.size() + " 筆):");
        combinedResults.forEach(System.out::println);
    }
}