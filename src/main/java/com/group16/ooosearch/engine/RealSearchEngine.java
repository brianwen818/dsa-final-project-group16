package com.group16.ooosearch.engine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.group16.ooosearch.fetch.PageCache;
import com.group16.ooosearch.fetch.PageFetcher;
import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.Passage;
import com.group16.ooosearch.model.WebPage;
import com.group16.ooosearch.scoring.QueryAwarePageScorer;
import com.group16.ooosearch.search.CachedGoogleProvider;
import com.group16.ooosearch.search.CandidateFinder;
import com.group16.ooosearch.search.DuckDuckGoProvider;
import com.group16.ooosearch.search.MediaWikiProvider;
import com.group16.ooosearch.search.QueryScope;
import com.group16.ooosearch.search.SearchProvider;
import com.group16.ooosearch.semantic.SemanticKeywordExpander;
import com.group16.ooosearch.snippet.ImageFinder;
import com.group16.ooosearch.snippet.PassageRanker;
import com.group16.ooosearch.tree.RealSiteRanker;
import com.group16.ooosearch.tree.SiteCrawler;
import com.group16.ooosearch.tree.SiteResult;
import com.group16.ooosearch.tree.SiteResults;
import com.group16.ooosearch.tree.WebNode;

/**
 * 正式版搜尋引擎：把 Stage 1–4 與段落摘要串成一次完整的搜尋。Stage 5 的網站只呼叫這個類別。
 *
 * <ol>
 *   <li>查詢限定在 OOO 範圍內，找候選網址（{@link CandidateFinder}）。</li>
 *   <li>爬子頁面建樹，單頁分數 = OOO 分數 × 查詢詞相關度（{@link QueryAwarePageScorer}），依網站總分排名。</li>
 *   <li>從前幾名網站挑出相關段落（{@link PassageRanker}），交給 Stage 4 找擴充詞（{@link SemanticKeywordExpander}）。</li>
 *   <li>用前 2 個新擴充詞再搜尋，新網址加入候選；以「OOO 關鍵字 + 擴充詞」重新計分排名。最多 2 輪。</li>
 *   <li>最終結果：去除重新導向的重複網站，前 10 名附上相關段落，前 5 名的段落再找圖片（{@link ImageFinder}）。</li>
 * </ol>
 * 已抓過的網頁都在快取中，重新計分時不必重新連線；只有新網址需要連網。
 */
public class RealSearchEngine implements SearchEngine {
    public static final Path KEYWORDS_FILE = Path.of("data", "keywords.txt");
    public static final Path QUERY_SYNONYMS_FILE = Path.of("data", "query-synonyms.txt");
    public static final Path STOPWORDS_FILE = Path.of("data", "stopwords.txt");
    public static final Path EXPANSION_EXCLUDES_FILE = Path.of("data", "expansion-excludes.txt");

    public static final int MAX_DEPTH = 2;
    public static final int MAX_PAGES_PER_TREE = 15;
    public static final int MAX_CHILDREN_PER_NODE = 5;
    /** 前幾名網站要附上段落；也是擴充詞的來源。 */
    public static final int PASSAGE_SITES = 10;
    /** 前幾名網站的段落要找圖片（每張圖都可能要連到 Fandom）。 */
    public static final int IMAGE_SITES = 5;
    /** 每輪擴充後，用前幾個新擴充詞再搜尋。 */
    public static final int NEW_TERMS_PER_ROUND = 2;
    public static final int MORE_RESULTS_PER_PROVIDER = 3;

    /** 一輪擴充的紀錄：新擴充詞（含權重）、相關度、再搜尋找到的新網址。 */
    public record ExpansionRound(int round, List<Keyword> terms, Map<String, Double> relevance, List<String> newUrls) {
    }

    private final PageFetcher fetcher;
    private final CandidateFinder finder;
    private final KeywordList baseKeywords;
    private final Path synonymsFile;
    private final SemanticKeywordExpander expander;
    private final ImageFinder imageFinder;
    private Consumer<String> progress = message -> { };
    private List<ExpansionRound> lastRounds = List.of();

    public RealSearchEngine(PageFetcher fetcher, SearchProvider google, List<SearchProvider> scopedProviders,
                            KeywordList keywords, Path synonymsFile, SemanticKeywordExpander expander) {
        this.fetcher = fetcher;
        this.finder = new CandidateFinder(google, scopedProviders);
        this.baseKeywords = keywords;
        this.synonymsFile = synonymsFile;
        this.expander = expander;
        this.imageFinder = new ImageFinder(fetcher);
    }

    /** 使用 data/ 下的檔案、網頁快取 data/cache，以及 Google 快取、中文維基、DuckDuckGo 三個來源。 */
    public static RealSearchEngine createDefault() throws IOException {
        PageFetcher fetcher = new PageFetcher(new PageCache());
        KeywordList keywords = KeywordList.fromFile(KEYWORDS_FILE);
        return new RealSearchEngine(fetcher, new CachedGoogleProvider(),
                List.of(new MediaWikiProvider(fetcher), new DuckDuckGoProvider(fetcher)),
                keywords, QUERY_SYNONYMS_FILE,
                new SemanticKeywordExpander(STOPWORDS_FILE, EXPANSION_EXCLUDES_FILE, keywords));
    }

    /** 設定進度訊息的輸出（例如 System.out::println）；預設不輸出。 */
    public RealSearchEngine onProgress(Consumer<String> progress) {
        this.progress = progress;
        return this;
    }

    /** 最近一次搜尋的擴充紀錄（命令列 Demo 與報告用）。 */
    public List<ExpansionRound> getLastRounds() {
        return lastRounds;
    }

    @Override
    public SearchResponse search(String query) {
        try {
            return run(query.strip());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private SearchResponse run(String query) throws IOException {
        Keyword topic = QueryScope.findTopic(baseKeywords);
        Keyword queryKeyword = QueryScope.queryKeyword(query, synonymsFile, baseKeywords);
        List<Keyword> exclusions = QueryScope.queryExclusions(query, synonymsFile);
        PassageRanker passageRanker = new PassageRanker(queryKeyword, exclusions, topic);

        // 1. 候選網址
        Set<String> urls = new LinkedHashSet<>(finder.find(query, queryKeyword, topic));
        progress.accept("候選網址 " + urls.size() + " 個（查詢寫法：" + queryKeyword.getForms() + "）");

        // 2. 建樹排名
        KeywordList keywords = baseKeywords;
        List<SiteResult> results = rank(urls, keywords, queryKeyword, exclusions);

        // 3–4. 從段落找擴充詞，再搜尋、重新排名（最多 2 輪）
        List<Keyword> expanded = new ArrayList<>();
        List<ExpansionRound> rounds = new ArrayList<>();
        for (int round = 1; round <= SemanticKeywordExpander.MAX_ITERATIONS; round++) {
            List<WebPage> passagePages = passagePages(results, keywords, passageRanker);
            if (passagePages.isEmpty()) {
                break;
            }
            KeywordList input = new KeywordList(keywords.toList());
            input.add(queryKeyword); // 查詢詞也算已知詞：不會被擴充，也能當共現圖的起點
            expander.expand(passagePages, input, crawledPages(results)); // DF 以所有爬到的網頁計算
            List<Keyword> newTerms = new ArrayList<>(expander.getLastExpandedKeywords());
            if (newTerms.isEmpty()) {
                break;
            }
            expanded.addAll(newTerms);
            List<Keyword> merged = new ArrayList<>(keywords.toList());
            merged.addAll(newTerms);
            keywords = new KeywordList(merged);

            List<String> terms = new ArrayList<>();
            for (int i = 0; i < Math.min(NEW_TERMS_PER_ROUND, newTerms.size()); i++) {
                terms.add(newTerms.get(i).getName());
            }
            List<String> newUrls = new ArrayList<>();
            for (String url : finder.findMore(terms, topic, MORE_RESULTS_PER_PROVIDER)) {
                if (urls.add(url)) {
                    newUrls.add(url);
                }
            }
            rounds.add(new ExpansionRound(round, newTerms, expander.getLastRelevance(), newUrls));
            progress.accept("第 " + round + " 輪擴充：" + names(newTerms) + "，再搜尋找到 " + newUrls.size() + " 個新網址");
            results = rank(urls, keywords, queryKeyword, exclusions);
        }
        lastRounds = List.copyOf(rounds);

        // 5. 最終結果、段落與圖片
        results = SiteResults.sortScorableFirst(SiteResults.dropRedirectDuplicates(results));
        Map<String, List<Passage>> passages = new LinkedHashMap<>();
        Set<String> shown = new HashSet<>(); // 跨網站去重：前面網站已顯示的段落，後面不再重複
        int rank = 0;
        for (SiteResult site : results) {
            if (!hasScore(site) || rank >= PASSAGE_SITES) {
                break;
            }
            List<Passage> top = passageRanker.topPassages(site, keywords, PassageRanker.DEFAULT_TOP, shown);
            if (rank < IMAGE_SITES) {
                List<Passage> withImages = new ArrayList<>();
                for (Passage passage : top) {
                    withImages.add(imageFinder.addImage(passage, keywords));
                }
                top = withImages;
            }
            passages.put(site.getRootUrl(), top);
            rank++;
        }
        progress.accept("完成：" + results.size() + " 個網站，擴充詞 " + names(expanded));
        return new SearchResponse(query, keywords, results, expanded, passages);
    }

    private List<SiteResult> rank(Set<String> urls, KeywordList keywords, Keyword queryKeyword, List<Keyword> exclusions) {
        progress.accept("爬取與計分 " + urls.size() + " 個網站…");
        QueryAwarePageScorer scorer = new QueryAwarePageScorer(queryKeyword, exclusions);
        SiteCrawler crawler = new SiteCrawler(MAX_DEPTH, MAX_PAGES_PER_TREE, MAX_CHILDREN_PER_NODE, fetcher, scorer);
        return new RealSiteRanker(crawler).rank(new ArrayList<>(urls), keywords);
    }

    /** 前幾名網站的相關段落，各包成一個小網頁交給 Stage 4（擴充器的介面吃 WebPage）。 */
    private static List<WebPage> passagePages(List<SiteResult> results, KeywordList keywords, PassageRanker ranker) {
        List<WebPage> pages = new ArrayList<>();
        Set<String> shown = new HashSet<>();
        int count = 0;
        for (SiteResult site : SiteResults.dropRedirectDuplicates(results)) {
            if (!hasScore(site) || count >= PASSAGE_SITES) {
                continue;
            }
            count++;
            for (Passage passage : ranker.topPassages(site, keywords, PassageRanker.DEFAULT_TOP, shown)) {
                pages.add(new WebPage(passage.anchorUrl(), passage.pageTitle(), "<p>" + escape(passage.text()) + "</p>"));
            }
        }
        return pages;
    }

    /** 所有爬到的網頁（同一網址只算一次），當作 Stage 4 計算 DF 的背景語料。 */
    private static List<WebPage> crawledPages(List<SiteResult> results) {
        Map<String, WebPage> pages = new LinkedHashMap<>();
        for (SiteResult site : results) {
            collectPages(site.getTree().getRoot(), pages);
        }
        return new ArrayList<>(pages.values());
    }

    private static void collectPages(WebNode node, Map<String, WebPage> pages) {
        if (node.getPageResult() != null) {
            pages.putIfAbsent(node.getWebPage().getUrl(), node.getWebPage());
        }
        for (WebNode child : node.getChildren()) {
            collectPages(child, pages);
        }
    }

    /** 0 分的網站（例如只講貓、沒提到 OOO）沒有值得說明的段落。 */
    private static boolean hasScore(SiteResult site) {
        return SiteResults.isScorable(site) && site.getTotalScore() > 0;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static List<String> names(List<Keyword> keywords) {
        List<String> names = new ArrayList<>();
        for (Keyword keyword : keywords) {
            names.add(keyword.getName());
        }
        return names;
    }
}
