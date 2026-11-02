package com.group16.ooosearch.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.group16.ooosearch.fetch.PageCache;
import com.group16.ooosearch.fetch.PageFetcher;
import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.WebPage;
import com.group16.ooosearch.search.CachedGoogleProvider;
import com.group16.ooosearch.search.DuckDuckGoProvider;
import com.group16.ooosearch.search.MediaWikiProvider;
import com.group16.ooosearch.search.SearchAggregator;
import com.group16.ooosearch.search.SearchProvider;
import com.group16.ooosearch.semantic.SemanticKeywordExpander;

/**
 * Stage 4 命令列 Demo：給定查詢關鍵字，經由 Stage 3 抓回網頁後，
 * 透過語意分析與共現次數計算（SemanticKeywordExpander）自動擴充相關關鍵字，
 * 並印出擴充前後的對比及明細。
 *
 * 用法：Stage4Main [查詢字串]
 * 預設查詢為「假面騎士OOO」；結果另存到 data/results/stage4-results.csv。
 */
public class Stage4Main {
    private static final String DEFAULT_QUERY = "假面騎士OOO";
    private static final Path KEYWORDS_FILE = Path.of("data", "keywords.txt");
    private static final Path STOPWORDS_FILE = Path.of("data", "stopwords.txt");
    private static final Path RESULTS_FILE = Path.of("data", "results", "stage4-results.csv");

    public static void main(String[] args) throws IOException {
        String query = args.length > 0 ? String.join(" ", args) : DEFAULT_QUERY;

        System.out.println("==================================================");
        System.out.println("    Stage 4: 語意分析與自動關鍵字擴充系統");
        System.out.println("==================================================\n");

        System.out.println("🔍 查詢主題: [" + query + "]");
        
        // 1. 載入原始關鍵字與停用詞
        KeywordList originalKeywords = KeywordList.fromFile(KEYWORDS_FILE);
        System.out.println("📦 已載入原始關鍵字共 " + originalKeywords.size() + " 個。");

        // 2. 初始化 Stage 3 的搜尋聚合器來抓取對應網頁內容
        PageCache cache = new PageCache();
        PageFetcher fetcher = new PageFetcher(cache);
        SearchProvider googleProvider = new CachedGoogleProvider();
        SearchProvider wikiProvider = new MediaWikiProvider(fetcher);
        SearchProvider ddgProvider = new DuckDuckGoProvider(fetcher);

        SearchAggregator aggregator = new SearchAggregator(List.of(googleProvider, wikiProvider, ddgProvider));
        List<String> targetUrls = aggregator.search(query, 5);
        System.out.println("🌐 透過搜尋聚合器取得 " + targetUrls.size() + " 筆目標網址進行語意分析..."); // 修正處：使用 targetUrls.size()

        // 實際抓取網頁內容
        List<WebPage> pages = new ArrayList<>();
        for (String url : targetUrls) {
            try {
                WebPage page = fetcher.fetch(url);
                pages.add(page);
                System.out.println("  - 成功抓取分析源: " + page.getTitle() + " (" + url + ")");
            } catch (Exception e) {
                System.err.println("  - 略過無法抓取的網址: " + url + " (" + e.getMessage() + ")");
            }
        }

        // 3. 執行 Stage 4 語意分析與關鍵字擴充
        System.out.println("\n⚙️ 正在執行斷詞、TF-IDF 與共現詞統計...");
        SemanticKeywordExpander expander = new SemanticKeywordExpander(STOPWORDS_FILE);
        KeywordList expandedKeywordsList = expander.expand(pages, originalKeywords);

        // 取得純擴充出的新關鍵字清單
        List<Keyword> newExpanded = expander.getLastExpandedKeywords();

        // 4. 印出擴充結果對比
        System.out.println("\n==================================================");
        System.out.println("               關鍵字擴充結果比對");
        System.out.println("==================================================");
        System.out.println("原始關鍵字數量: " + originalKeywords.size());
        System.out.println("擴充後總關鍵字數: " + expandedKeywordsList.size());
        System.out.println("✨ 本次自動挖掘出的擴充關鍵字 (" + newExpanded.size() + " 個):");
        if (newExpanded.isEmpty()) {
            System.out.println("  （無符合門檻的新擴充詞）");
        } else {
            for (int i = 0; i < newExpanded.size(); i++) {
                Keyword kw = newExpanded.get(i);
                System.out.printf("  %d. %s (權重: %.1f)\n", (i + 1), kw.getName(), kw.getWeight());
            }
        }
        System.out.println("==================================================\n");

        // 5. 輸出結果至 CSV
        StringBuilder csv = new StringBuilder("\uFEFF擴充詞名次,擴充關鍵字,賦予權重,來源查詢\n");
        for (int i = 0; i < newExpanded.size(); i++) {
            Keyword kw = newExpanded.get(i);
            csv.append(i + 1).append(',')
               .append(csvCell(kw.getName())).append(',')
               .append(kw.getWeight()).append(',')
               .append(csvCell(query)).append('\n');
        }

        Files.createDirectories(RESULTS_FILE.getParent());
        Files.writeString(RESULTS_FILE, csv, StandardCharsets.UTF_8);
        System.out.println("🎉 Stage 4 執行完成！擴充結果已存到 " + RESULTS_FILE);
    }

    private static String csvCell(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}