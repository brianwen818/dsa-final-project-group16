package com.group16.ooosearch.cli;

import com.group16.ooosearch.fetch.PageCache;
import com.group16.ooosearch.fetch.PageFetcher;
import com.group16.ooosearch.search.*;
import com.group16.ooosearch.tree.SiteCrawler;
import com.group16.ooosearch.tree.WebTree;

import java.util.*;

public class Stage3Main {

    // 封裝比對結果用的資料結構
    private static class RankedResult {
        String url;
        String title;
        int googleRank;  // -1 表示非 Google 原始結果 (如來自 MediaWiki/DDG 補充)
        double ourScore;
        int ourRank;

        RankedResult(String url, String title, int googleRank, double ourScore) {
            this.url = url;
            this.title = title;
            this.googleRank = googleRank;
            this.ourScore = ourScore;
        }
    }

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   Stage 3: 關鍵字搜尋與 Stage 2 重新排名對比系統");
        System.out.println("==================================================\n");

        PageCache cache = new PageCache();
        PageFetcher fetcher = new PageFetcher(cache);

        // 1. 初始化 Provider
        SearchProvider googleProvider = new CachedGoogleProvider();
        SearchProvider wikiProvider = new MediaWikiProvider(fetcher);
        SearchProvider ddgProvider = new DuckDuckGoProvider(fetcher);

        // 記錄 Google 原始排名 (URL -> Rank)
        String query = "假面騎士OOO";
        List<String> googleOriginalList = googleProvider.search(query, 10);
        Map<String, Integer> googleRankMap = new HashMap<>();
        for (int i = 0; i < googleOriginalList.size(); i++) {
            googleRankMap.put(googleOriginalList.get(i), i + 1);
        }

        // 2. 透過 SearchAggregator 進行多源搜尋與 LinkedHashSet 自動去重
        SearchAggregator aggregator = new SearchAggregator(List.of(googleProvider, wikiProvider, ddgProvider));
        List<String> aggregatedUrls = aggregator.search(query, 5);

        System.out.println("🔍 搜尋關鍵字: [" + query + "]");
        System.out.println("✅ 已透過 SearchAggregator 取得 " + aggregatedUrls.size() + " 筆去重後目標網址。");
        System.out.println("--------------------------------------------------\n");

        // 3. 呼叫 Stage 2 SiteCrawler 進行爬取與建樹計分
        System.out.println("🚀 執行 Stage 2 爬蟲與 WebTree 建樹計分中...");
        SiteCrawler crawler = new SiteCrawler(2, 15);
        List<RankedResult> resultsList = new ArrayList<>();

        for (String url : aggregatedUrls) {
            try {
                WebTree tree = crawler.crawl(url);
                String title = url;
                double score = 0.0;

                if (tree != null && tree.getRoot() != null) {
                    if (tree.getRoot().getWebPage() != null && tree.getRoot().getWebPage().getTitle() != null) {
                        title = tree.getRoot().getWebPage().getTitle();
                    }
                    score = tree.getRoot().getNodeScore();
                }

                int gRank = googleRankMap.getOrDefault(url, -1);
                resultsList.add(new RankedResult(url, title, gRank, score));

            } catch (Exception e) {
                System.err.println("⚠️ 爬取失敗 (" + url + "): " + e.getMessage());
            }
        }

        // 4. 根據 Stage 2 算出的我們的分數 (ourScore) 由高到低重新排名 (Re-rank)
        resultsList.sort((a, b) -> Double.compare(b.ourScore, a.ourScore));
        for (int i = 0; i < resultsList.size(); i++) {
            resultsList.get(i).ourRank = i + 1;
        }

        // 5. 印出對比表格 (對齊 Demo / 報告規格要求)
        System.out.println("\n==========================================================================================================");
        System.out.printf("%-6s | %-10s | %-10s | %-40s | %s\n", "我們的排名", "Google 排名", "我們的分數", "網頁標題 / 說明", "網址");
        System.out.println("----------------------------------------------------------------------------------------------------------");

        for (RankedResult res : resultsList) {
            String gRankStr = (res.googleRank == -1) ? "無 (其他來源)" : String.valueOf(res.googleRank);
            String displayTitle = res.title.length() > 38 ? res.title.substring(0, 35) + "..." : res.title;
            
            System.out.printf("  %-8d | %-12s | %-12.2f | %-40s | %s\n",
                    res.ourRank,
                    gRankStr,
                    res.ourScore,
                    displayTitle,
                    res.url
            );
        }
        System.out.println("==========================================================================================================\n");
        System.out.println("🎉 Stage 3 搜尋與 Google 排名比對完成！");
    }
}