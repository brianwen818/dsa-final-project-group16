package com.group16.ooosearch.cli;

import com.group16.ooosearch.fetch.PageCache;
import com.group16.ooosearch.fetch.PageFetcher;
import com.group16.ooosearch.fetch.UrlCanonicalizer;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.PageResult;
import com.group16.ooosearch.search.*;
import com.group16.ooosearch.tree.RealSiteRanker;
import com.group16.ooosearch.tree.SiteResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Stage3Main {
    private static final String DEFAULT_QUERY = "假面騎士OOO";
    private static final Path KEYWORDS_FILE = Path.of("data", "keywords.txt");
    private static final Path RESULTS_FILE = Path.of("data", "results", "stage3-results.csv");

    /** 查詢字串由命令列參數指定，例如 -Dexec.args="火野映司"；沒有指定時查「假面騎士OOO」。 */
    public static void main(String[] args) throws IOException {
        String query = args.length > 0 ? String.join(" ", args) : DEFAULT_QUERY;

        System.out.println("==================================================");
        System.out.println("   Stage 3: 關鍵字搜尋與 Stage 2 重新排名對比系統");
        System.out.println("==================================================\n");

        PageCache cache = new PageCache();
        PageFetcher fetcher = new PageFetcher(cache);

        // 1. 初始化 Provider
        SearchProvider googleProvider = new CachedGoogleProvider();
        SearchProvider wikiProvider = new MediaWikiProvider(fetcher);
        SearchProvider ddgProvider = new DuckDuckGoProvider(fetcher);

        // 記錄 Google 原始排名 (正規化後的 URL -> Rank)
        List<String> googleOriginalList = googleProvider.search(query, 10);
        Map<String, Integer> googleRankMap = new HashMap<>();
        for (int i = 0; i < googleOriginalList.size(); i++) {
            googleRankMap.putIfAbsent(UrlCanonicalizer.canonicalize(googleOriginalList.get(i)), i + 1);
        }

        // 2. 透過 SearchAggregator 進行多源搜尋、網址正規化與 LinkedHashSet 自動去重
        SearchAggregator aggregator = new SearchAggregator(List.of(googleProvider, wikiProvider, ddgProvider));
        List<String> aggregatedUrls = aggregator.search(query, 5);

        System.out.println("🔍 搜尋關鍵字: [" + query + "]");
        System.out.println("✅ 已透過 SearchAggregator 取得 " + aggregatedUrls.size() + " 筆去重後目標網址。");
        System.out.println("--------------------------------------------------\n");

        // 3. 套用 Stage 2 的排名演算法：BFS 建樹、後序走訪算總分、由高到低排序
        System.out.println("🚀 執行 Stage 2 爬蟲與 WebTree 建樹計分中...");
        KeywordList keywords = KeywordList.fromFile(KEYWORDS_FILE);
        List<SiteResult> results = new RealSiteRanker(2, 15).rank(aggregatedUrls, keywords);

        // 4. 印出對比表格，抓不到內容的網站排在最後並標示為無法評分
        System.out.println("\n==========================================================================================================");
        System.out.printf("%-6s | %-10s | %-10s | %-40s | %s\n", "我們的排名", "Google 排名", "我們的分數", "網頁標題 / 說明", "網址");
        System.out.println("----------------------------------------------------------------------------------------------------------");

        StringBuilder csv = new StringBuilder("﻿查詢,我們的排名,Google 排名,我們的分數,節點數,標題,網址\n");
        int rank = 0;
        for (SiteResult result : sortScorableFirst(results)) {
            boolean scorable = isScorable(result);
            String ourRank = scorable ? String.valueOf(++rank) : "-";
            Integer googleRank = googleRankMap.get(UrlCanonicalizer.canonicalize(result.getRootUrl()));
            String googleRankText = googleRank == null ? "無 (其他來源)" : googleRank.toString();
            String score = scorable ? String.format("%.2f", result.getTotalScore()) : "無法評分";
            String title = result.getTree().getRoot().getWebPage().getTitle();
            String displayTitle = title.length() > 38 ? title.substring(0, 35) + "..." : title;

            System.out.printf("  %-8s | %-12s | %-12s | %-40s | %s\n",
                    ourRank, googleRankText, score, displayTitle, result.getRootUrl());
            csv.append(csvCell(query)).append(',').append(ourRank).append(',')
               .append(googleRank == null ? "" : googleRank).append(',').append(score).append(',')
               .append(result.getTree().size()).append(',').append(csvCell(title)).append(',')
               .append(csvCell(result.getRootUrl())).append('\n');
        }
        System.out.println("==========================================================================================================\n");

        Files.createDirectories(RESULTS_FILE.getParent());
        Files.writeString(RESULTS_FILE, csv, StandardCharsets.UTF_8);
        System.out.println("🎉 Stage 3 搜尋與 Google 排名比對完成！結果已存到 " + RESULTS_FILE);
    }

    private static boolean isScorable(SiteResult result) {
        PageResult rootResult = result.getTree().getRoot().getPageResult();
        return rootResult != null;
    }

    private static List<SiteResult> sortScorableFirst(List<SiteResult> results) {
        List<SiteResult> sorted = new ArrayList<>(results);
        sorted.sort(Comparator.comparing((SiteResult r) -> !isScorable(r))); // 穩定排序，保留分數順序
        return sorted;
    }

    private static String csvCell(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
