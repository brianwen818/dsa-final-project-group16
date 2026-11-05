package com.group16.ooosearch.snippet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.Passage;
import com.group16.ooosearch.model.PageResult;
import com.group16.ooosearch.model.WebPage;
import com.group16.ooosearch.tree.SiteResult;
import com.group16.ooosearch.tree.WebNode;
import com.group16.ooosearch.tree.WebTree;

class PassageRankerTest {
    private final KeywordList keywords = new KeywordList(List.of(
            new Keyword("假面騎士OOO", 5, List.of("OOO")),
            new Keyword("卡佳里", 4, List.of("Kazari")),
            new Keyword("獅虎豹組合", 3, List.of("獅虎豹", "貓系聯組"))));
    private final PassageRanker ranker = new PassageRanker(
            new Keyword("貓咪", 1, List.of("貓")), List.of(new Keyword("熊貓", 0)),
            new Keyword("假面騎士OOO", 5, List.of("OOO")));

    private static WebPage page(String url, String body) {
        return page(url, "標題", body);
    }

    private static WebPage page(String url, String title, String body) {
        return new WebPage(url, title, "<html><body>" + body + "</body></html>");
    }

    @Test
    void onlyPassagesWithTheQueryAreChosenAndOooMakesThemRankHigher() {
        List<Passage> passages = ranker.rankPage(page("https://a.test/",
                "<h2 id=\"k\">卡佳里</h2><p>卡佳里（Kazari）是貓系的幹部，OOO 的敵人。</p>"
                + "<p>這一段完全沒有提到查詢詞，只講 OOO。</p>"
                + "<p>家裡的貓咪很可愛，每天都在睡覺。</p>"), keywords);

        // 「家裡的貓咪」沒有 OOO 關鍵字、網頁標題也沒提到 OOO，不入選
        assertEquals(1, passages.size());
        assertTrue(passages.get(0).text().startsWith("卡佳里"));
        assertEquals(List.of("卡佳里", "假面騎士OOO"), passages.get(0).tags()); // 章節標題中的卡佳里排前面
        assertEquals("https://a.test/#k", passages.get(0).anchorUrl());
    }

    @Test
    void passagesOnAnOooPageQualifyWithoutKeywords() {
        List<Passage> passages = ranker.rankPage(page("https://a.test/", "假面騎士OOO",
                "<p>03 ネコと進化と食いしん坊 貓‧進化‧貪吃鬼</p><p>卡佳里（Kazari）是貓系的幹部。</p>"), keywords);

        assertEquals(2, passages.size());
        assertTrue(passages.get(0).score() > passages.get(1).score()); // 有 OOO 關鍵字的排前面
        assertTrue(passages.get(1).text().contains("貪吃鬼"));
    }

    @Test
    void exclusionWordsDoNotCountAsTheQuery() {
        List<Passage> passages = ranker.rankPage(page("https://a.test/",
                "<p>PANDA（熊貓）核心硬幣，OOO 的手臂。</p>"), keywords);

        assertTrue(passages.isEmpty());
    }

    @Test
    void topPassagesComeFromTheWholeTreeAndAreDeduplicated() {
        WebPage root = page("https://site.test/root",
                "<p>OOO 的獅虎豹 貓系聯組，頭部能放出強光。</p><p>OOO 的獅虎豹 貓系聯組，頭部能放出強光。</p>");
        WebPage child = page("https://site.test/kazari", "假面騎士OOO角色列表",
                "<p>卡佳里是貓系幹部，代表色是黃色，是 OOO 的敵人。</p><p>貓系硬幣一共有三枚。</p><p>還有一隻貓出場。</p>");
        WebTree tree = new WebTree(root);
        tree.getRoot().setPageResult(new PageResult(root, 1, java.util.Map.of()));
        WebNode node = new WebNode(child);
        node.setPageResult(new PageResult(child, 1, java.util.Map.of()));
        tree.getRoot().addChild(node);

        List<Passage> top = ranker.topPassages(new SiteResult("https://site.test/root", 1, tree), keywords, 3);

        assertEquals(3, top.size());
        long lionRows = top.stream().filter(p -> p.text().contains("獅虎豹")).count();
        assertEquals(1, lionRows); // 重複的段落只留一個
        assertTrue(top.get(0).score() >= top.get(1).score() && top.get(1).score() >= top.get(2).score());
        assertFalse(top.stream().anyMatch(p -> p.text().contains("熊貓")));
    }

    @Test
    void longPassagesAreTrimmedAroundTheQuery() {
        String filler = "這是一段很長的介紹文字".repeat(30);
        String snippet = ranker.snippet(filler + "卡佳里是貓系幹部" + filler);

        assertTrue(snippet.contains("貓系幹部"));
        assertTrue(snippet.startsWith("…") && snippet.endsWith("…"));
        assertTrue(snippet.length() <= 2 * PassageRanker.SNIPPET_RADIUS + 2);
    }
}
