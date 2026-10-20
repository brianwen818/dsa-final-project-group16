package com.group16.ooosearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.group16.ooosearch.engine.FakeSearchEngine;
import com.group16.ooosearch.engine.SearchResponse;
import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.PageResult;
import com.group16.ooosearch.model.WebPage;
import com.group16.ooosearch.scoring.FakePageScorer;
import com.group16.ooosearch.search.FakeSearchProvider;
import com.group16.ooosearch.semantic.FakeKeywordExpander;
import com.group16.ooosearch.tree.FakeSiteRanker;
import com.group16.ooosearch.tree.SiteResult;

/** 確認每個假實作都回傳格式正確的結果，讓各 Stage 可以放心拿來開發。 */
class FakeImplementationsTest {

    private final KeywordList keywords = new KeywordList(List.of(
            new Keyword("假面騎士OOO", 5), new Keyword("安卡", 3)));

    @Test
    void fakePageScorerCountsEveryKeyword() {
        PageResult result = new FakePageScorer().score(new WebPage("https://example.com", "", ""), keywords);

        assertEquals(FakePageScorer.FAKE_SCORE, result.getScore());
        assertEquals(2, result.getKeywordCounts().size());
        assertEquals(0, result.getCount("安卡"));
    }

    @Test
    void fakeSiteRankerReturnsOneTreePerUrl() {
        List<SiteResult> results = new FakeSiteRanker().rank(List.of("https://a.com", "https://b.com"), keywords);

        assertEquals(2, results.size());
        assertEquals("https://a.com", results.get(0).getRootUrl());
        assertEquals(1, results.get(0).getTree().size());
    }

    @Test
    void fakeSearchProviderRespectsLimit() {
        assertEquals(3, new FakeSearchProvider().search("安卡", 3).size());
    }

    @Test
    void fakeKeywordExpanderKeepsOriginalKeywords() {
        KeywordList expanded = new FakeKeywordExpander().expand(List.of(), keywords);
        assertEquals(keywords.size(), expanded.size());
    }

    @Test
    void fakeSearchEngineReturnsCompleteResponse() {
        SearchResponse response = new FakeSearchEngine().search("安卡 結局");

        assertEquals(2, response.getKeywords().size());
        assertFalse(response.getResults().isEmpty());
        assertFalse(response.getExpandedKeywords().isEmpty());
    }
}
