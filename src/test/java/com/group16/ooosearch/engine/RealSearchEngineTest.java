package com.group16.ooosearch.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.group16.ooosearch.fetch.PageCache;
import com.group16.ooosearch.fetch.PageFetcher;
import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.Passage;
import com.group16.ooosearch.search.SearchProvider;
import com.group16.ooosearch.semantic.SemanticKeywordExpander;
import com.group16.ooosearch.tree.SiteResult;

/** 網頁都預先放進快取、搜尋來源是假的，因此測試不連網路。 */
class RealSearchEngineTest {
    private static final String SITE = "https://site.test/";

    @TempDir
    Path dir;

    private PageCache cache;
    private RealSearchEngine engine;
    private final List<String> scopedQueries = new ArrayList<>();

    // 卡佳里沒有英文寫法，ImageFinder 不會連到 Fandom
    private final KeywordList keywords = new KeywordList(List.of(
            new Keyword("假面騎士OOO", 5, List.of("OOO")),
            new Keyword("卡佳里", 3.7, List.of()),
            new Keyword("獅虎豹組合", 2.9, List.of("獅虎豹", "貓系聯組"))));

    @BeforeEach
    void setUp() throws IOException {
        cache = new PageCache(dir.resolve("cache"));
        page("ooo", "<h2 id=\"k\">卡佳里</h2><p>卡佳里是 OOO 的貓系幹部，代表色是黃色。</p>"
                + "<h2 id=\"c\">聯組</h2><p>OOO 的獅虎豹是貓系聯組，頭部能放出強光。</p>");
        page("pets", "<p>貓咪很可愛，貓喜歡睡覺，貓咪也喜歡玩。</p>");
        page("plain", "<p>OOO 是 2010 年播出的特攝作品，主角是火野映司。</p>");

        Path synonyms = dir.resolve("query-synonyms.txt");
        Files.writeString(synonyms, "貓咪 | 貓 | !熊貓\n", StandardCharsets.UTF_8);

        SearchProvider google = (q, limit) -> List.of(SITE + "pets");
        SearchProvider scoped = (q, limit) -> {
            scopedQueries.add(q);
            return List.of(SITE + "ooo", SITE + "plain");
        };
        engine = new RealSearchEngine(new PageFetcher(cache, 0, 200), google, List.of(scoped), keywords, synonyms,
                new SemanticKeywordExpander(Path.of("data", "stopwords.txt"), null, keywords));
    }

    private void page(String path, String body) throws IOException {
        cache.put(SITE + path, "<html><head><title>" + path + "</title></head><body>" + body + "</body></html>");
    }

    @Test
    void ranksPagesAboutBothOooAndTheQueryFirstAndExplainsWhy() {
        SearchResponse response = engine.search("貓咪");

        List<SiteResult> results = response.getResults();
        assertEquals(SITE + "ooo", results.get(0).getRootUrl());
        SiteResult pets = results.stream().filter(r -> r.getRootUrl().equals(SITE + "pets")).findFirst().orElseThrow();
        assertEquals(0.0, pets.getTotalScore()); // 只講貓、沒提到 OOO

        List<Passage> passages = response.getPassages(SITE + "ooo");
        assertFalse(passages.isEmpty());
        assertTrue(passages.stream().allMatch(p -> p.text().contains("貓")));
        assertTrue(passages.stream().anyMatch(p -> p.tags().contains("卡佳里")));
        assertTrue(response.getPassages(SITE + "pets").isEmpty());

        // 送給外部來源的查詢都加上了主題名稱
        assertTrue(scopedQueries.contains("貓咪 假面騎士OOO"));
        assertTrue(engine.getLastRounds().size() <= SemanticKeywordExpander.MAX_ITERATIONS);
    }

    @Test
    void fakeEngineStillWorksForStage5() {
        SearchResponse fake = new FakeSearchEngine().search("貓咪");
        String first = fake.getResults().get(0).getRootUrl();
        assertEquals(1, fake.getPassages(first).size());
    }
}
