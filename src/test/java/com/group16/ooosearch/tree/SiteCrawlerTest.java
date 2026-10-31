package com.group16.ooosearch.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.group16.ooosearch.fetch.PageCache;
import com.group16.ooosearch.fetch.PageFetcher;
import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;

/** 網頁都預先放進快取，因此測試不連網路。 */
class SiteCrawlerTest {
    private static final String SITE = "https://site.test/";

    @TempDir
    Path cacheDir;

    private PageCache cache;
    private PageFetcher fetcher;
    private final KeywordList keywords = new KeywordList(List.of(new Keyword("OOO", 2.0)));

    @BeforeEach
    void setUp() {
        cache = new PageCache(cacheDir);
        fetcher = new PageFetcher(cache);
    }

    private void page(String path, String text, String... links) throws IOException {
        StringBuilder html = new StringBuilder("<html><body><p>" + text + "</p>");
        for (String link : links) {
            html.append("<a href=\"").append(link).append("\">x</a>");
        }
        cache.put(SITE + path, html.append("</body></html>").toString());
    }

    @Test
    void buildsTreeToMaxDepthAndDoesNotRevisitPages() throws IOException {
        page("root", "OOO OOO", "/a", "/root", "/a");
        page("a", "OOO", "/b", "/root");
        page("b", "OOO", "/c");
        page("c", "OOO");

        WebTree tree = new SiteCrawler(2, 15, 5, fetcher).crawl(SITE + "root", keywords);

        assertEquals(3, tree.size()); // root、a、b；c 超過深度 2
        WebNode b = tree.getRoot().getChildren().get(0).getChildren().get(0);
        assertEquals(2, b.getDepth());
        assertEquals(5.5, tree.calculateTotalScores(0.5), 1e-9); // 4 + 2×0.5 + 2×0.25
    }

    @Test
    void childLimitLetsBudgetReachSecondLevel() throws IOException {
        page("root", "OOO", "/a", "/b", "/c");
        page("a", "OOO", "/d");
        page("b", "OOO");
        page("c", "OOO");
        page("d", "OOO");

        WebTree tree = new SiteCrawler(2, 4, 2, fetcher).crawl(SITE + "root", keywords);

        assertEquals(2, tree.getRoot().getChildren().size());
        assertEquals(1, tree.getRoot().getChildren().get(0).getChildren().size());
        assertEquals(4, tree.size());
    }

    @Test
    void zeroScorePageIsNotExpanded() throws IOException {
        page("root", "OOO", "/empty");
        page("empty", "nothing", "/hidden");
        page("hidden", "OOO");

        WebTree tree = new SiteCrawler(3, 15, 5, fetcher).crawl(SITE + "root", keywords);

        assertEquals(2, tree.size());
    }

    @Test
    void ignoresLinksInMaintenanceBlocks() throws IOException {
        cache.put(SITE + "root", "<html><body><p>OOO</p><div class=\"ambox\"><a href=\"/notice\">x</a></div>"
                + "<nav><a href=\"/menu\">x</a></nav><a href=\"/a\">x</a></body></html>");
        page("a", "OOO");

        WebTree tree = new SiteCrawler(2, 15, 5, fetcher).crawl(SITE + "root", keywords);

        assertEquals(2, tree.size());
    }

    @Test
    void rankerOrdersByTotalScoreUsingGivenKeywords() throws IOException {
        cache.put(SITE + "low", "<p>OOO</p>");
        cache.put(SITE + "high", "<p>OOO OOO OOO</p>");

        List<SiteResult> results = new RealSiteRanker(new SiteCrawler(2, 15, 5, fetcher))
                .rank(List.of(SITE + "low", SITE + "high"), keywords);

        assertEquals(SITE + "high", results.get(0).getRootUrl());
        assertEquals(6.0, results.get(0).getTotalScore(), 1e-9);
        assertEquals(2.0, results.get(1).getTotalScore(), 1e-9);
    }

    @Test
    void unreachableRootHasNoPageResult() {
        // 連到保留網域的不存在主機，只會得到連線失敗
        WebTree tree = new SiteCrawler(2, 15, 5, new PageFetcher(cache, 0, 200))
                .crawl("http://unreachable.invalid/", keywords);

        assertNull(tree.getRoot().getPageResult());
        assertEquals(1, tree.size());
    }
}
