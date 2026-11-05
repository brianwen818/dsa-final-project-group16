package com.group16.ooosearch.cli;

import com.group16.ooosearch.fetch.PageCache;
import com.group16.ooosearch.fetch.PageFetcher;
import com.group16.ooosearch.fetch.UrlCanonicalizer;
import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.scoring.QueryAwarePageScorer;
import com.group16.ooosearch.search.*;
import com.group16.ooosearch.tree.RealSiteRanker;
import com.group16.ooosearch.tree.SiteCrawler;
import com.group16.ooosearch.tree.SiteResult;
import com.group16.ooosearch.tree.SiteResults;
import com.group16.ooosearch.tree.WebNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Stage 3：只給關鍵字，自己找網站並重新排名。
 *
 * <ol>
 *   <li>查詢加上「假面騎士OOO」後送給維基搜尋與 DuckDuckGo（QueryScope、CandidateFinder）；Google 快取仍用原始查詢，當作對照組。</li>
 *   <li>每個候選網址用 Stage 2 的爬蟲建樹，單頁分數同時考慮 OOO 關鍵字與查詢詞（QueryAwarePageScorer）。</li>
 *   <li>依網站總分排名，並與 Google 原始排名比較。</li>
 * </ol>
 * 完整流程（含 Stage 4 擴充、段落摘要與圖片）見 {@link SearchMain}。
 */
public class Stage3Main {
    private static final String DEFAULT_QUERY = "假面騎士OOO";
    private static final Path KEYWORDS_FILE = Path.of("data", "keywords.txt");
    private static final Path QUERY_SYNONYMS_FILE = Path.of("data", "query-synonyms.txt");
    private static final Path RESULTS_FILE = Path.of("data", "results", "stage3-results.csv");
    private static final String CSV_HEADER = "查詢,我們的排名,Google 排名,我們的分數,節點數,查詢詞次數,標題,網址";

    /** 查詢字串由命令列參數指定，例如 -Dexec.args="貓咪"；沒有指定時查「假面騎士OOO」。 */
    public static void main(String[] args) throws IOException {
        String query = args.length > 0 ? String.join(" ", args) : DEFAULT_QUERY;

        System.out.println("==================================================");
        System.out.println("   Stage 3: 關鍵字搜尋與 Stage 2 重新排名對比系統");
        System.out.println("==================================================\n");

        PageFetcher fetcher = new PageFetcher(new PageCache());
        KeywordList keywords = KeywordList.fromFile(KEYWORDS_FILE);
        Keyword topic = QueryScope.findTopic(keywords);
        Keyword queryKeyword = QueryScope.queryKeyword(query, QUERY_SYNONYMS_FILE, keywords);
        List<Keyword> queryExclusions = QueryScope.queryExclusions(query, QUERY_SYNONYMS_FILE);

        // 1. 初始化 Provider
        SearchProvider googleProvider = new CachedGoogleProvider();
        SearchProvider wikiProvider = new MediaWikiProvider(fetcher);
        SearchProvider ddgProvider = new DuckDuckGoProvider(fetcher);

        // Google 對照組用原始查詢（組員手動記錄的就是原始查詢的結果）
        List<String> googleOriginalList = googleProvider.search(query, 10);
        Map<String, Integer> googleRankMap = new HashMap<>();
        for (int i = 0; i < googleOriginalList.size(); i++) {
            googleRankMap.putIfAbsent(UrlCanonicalizer.canonicalize(googleOriginalList.get(i)), i + 1);
        }

        // 2. 候選網址：Google 前幾筆，加上「查詢 + 假面騎士OOO」在維基與 DuckDuckGo 的結果
        CandidateFinder finder = new CandidateFinder(googleProvider, List.of(wikiProvider, ddgProvider));
        List<String> aggregatedUrls = finder.find(query, queryKeyword, topic);

        System.out.println("🔍 搜尋關鍵字: [" + query + "]，計分時的寫法: " + queryKeyword.getForms());
        System.out.println("🔍 送給維基與 DuckDuckGo 的查詢: "
                + QueryScope.scopedQueries(queryKeyword, topic, CandidateFinder.MAX_QUERY_VARIANTS));
        System.out.println("✅ 已取得 " + aggregatedUrls.size() + " 筆去重後目標網址。");
        System.out.println("--------------------------------------------------\n");

        // 3. 套用 Stage 2 的排名演算法，單頁分數同時考慮 OOO 關鍵字與查詢詞
        System.out.println("🚀 執行 Stage 2 爬蟲與 WebTree 建樹計分中...");
        QueryAwarePageScorer scorer = new QueryAwarePageScorer(queryKeyword, queryExclusions);
        SiteCrawler crawler = new SiteCrawler(2, 15, 5, fetcher, scorer);
        List<SiteResult> results = new RealSiteRanker(crawler).rank(aggregatedUrls, keywords);

        // 4. 印出對比表格，抓不到內容的網站排在最後並標示為無法評分
        System.out.println("\n==========================================================================================================");
        System.out.printf("%-6s | %-10s | %-10s | %-8s | %-40s | %s\n", "我們的排名", "Google 排名", "我們的分數", "查詢詞次數", "網頁標題 / 說明", "網址");
        System.out.println("----------------------------------------------------------------------------------------------------------");

        List<String> rows = new ArrayList<>();
        int rank = 0;
        for (SiteResult result : SiteResults.sortScorableFirst(SiteResults.dropRedirectDuplicates(results))) {
            boolean scorable = SiteResults.isScorable(result);
            String ourRank = scorable ? String.valueOf(++rank) : "-";
            Integer googleRank = googleRankMap.get(UrlCanonicalizer.canonicalize(result.getRootUrl()));
            String googleRankText = googleRank == null ? "無 (其他來源)" : googleRank.toString();
            String score = scorable ? String.format("%.2f", result.getTotalScore()) : "無法評分";
            int queryCount = countInTree(result.getTree().getRoot(), scorer.queryLabel());
            String title = result.getTree().getRoot().getWebPage().getTitle();
            String displayTitle = title.length() > 38 ? title.substring(0, 35) + "..." : title;

            System.out.printf("  %-8s | %-12s | %-12s | %-8d | %-40s | %s\n",
                    ourRank, googleRankText, score, queryCount, displayTitle, result.getRootUrl());
            rows.add(csvCell(query) + ',' + ourRank + ',' + (googleRank == null ? "" : googleRank) + ','
                    + score + ',' + result.getTree().size() + ',' + queryCount + ','
                    + csvCell(title) + ',' + csvCell(result.getRootUrl()));
        }
        System.out.println("==========================================================================================================\n");

        writeResults(query, rows);
        System.out.println("🎉 Stage 3 搜尋與 Google 排名比對完成！結果已存到 " + RESULTS_FILE);
    }

    /** 整棵網站樹中查詢詞出現的總次數。 */
    private static int countInTree(WebNode node, String label) {
        int sum = node.getPageResult() == null ? 0 : node.getPageResult().getCount(label);
        for (WebNode child : node.getChildren()) {
            sum += countInTree(child, label);
        }
        return sum;
    }

    /** 結果檔保留其他查詢的列，只取代這次查詢的列。 */
    private static void writeResults(String query, List<String> rows) throws IOException {
        List<String> kept = new ArrayList<>();
        if (Files.exists(RESULTS_FILE)) {
            List<String> lines = Files.readAllLines(RESULTS_FILE, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!line.isBlank() && !line.startsWith(csvCell(query) + ",")) {
                    kept.add(line);
                }
            }
        }
        kept.addAll(rows);
        StringBuilder csv = new StringBuilder("﻿").append(CSV_HEADER).append('\n');
        for (String line : kept) {
            csv.append(line).append('\n');
        }
        Files.createDirectories(RESULTS_FILE.getParent());
        Files.writeString(RESULTS_FILE, csv, StandardCharsets.UTF_8);
    }

    static String csvCell(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
