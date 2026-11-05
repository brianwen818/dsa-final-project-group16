package com.group16.ooosearch.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.PageResult;
import com.group16.ooosearch.model.WebPage;

class QueryAwarePageScorerTest {

    private final KeywordList keywords = new KeywordList(List.of(
            new Keyword("假面騎士OOO", 5, List.of("OOO")),
            new Keyword("卡佳里", 4, List.of("カザリ")),
            new Keyword("安卡拉", 0)));
    private final QueryAwarePageScorer scorer =
            new QueryAwarePageScorer(new Keyword("貓咪", 1, List.of("貓", "ネコ")));

    private PageResult score(String body) {
        return scorer.score(new WebPage("https://example.com", "", "<p>" + body + "</p>"), keywords);
    }

    @Test
    void pageWithoutOooScoresZeroEvenIfItMentionsTheQuery() {
        PageResult result = score("貓咪很可愛，貓喜歡睡覺。");

        assertEquals(0.0, result.getScore());
        assertEquals(2, result.getCount(scorer.queryLabel()));
    }

    @Test
    void oooPageWithoutQueryKeepsOnlyAlphaOfItsScore() {
        PageResult result = score("假面騎士OOO 的卡佳里。");

        assertEquals(0, result.getCount(scorer.queryLabel()));
        assertEquals((5 + 4) * QueryAwarePageScorer.ALPHA, result.getScore(), 1e-9);
    }

    @Test
    void mentioningBothRanksAboveMentioningOnlyOoo() {
        PageResult both = score("假面騎士OOO 的卡佳里是貓系的貪婪，ネコ。");
        PageResult oooOnly = score("假面騎士OOO 的卡佳里是貪婪。");

        assertEquals(2, both.getCount(scorer.queryLabel()));
        assertEquals((5 + 4) * (QueryAwarePageScorer.ALPHA + Math.log(3)), both.getScore(), 1e-9);
        assertTrue(both.getScore() > oooOnly.getScore());
    }

    @Test
    void queryCountRespectsExclusionWords() {
        QueryAwarePageScorer ankh = new QueryAwarePageScorer(new Keyword("安卡", 1));
        PageResult result = ankh.score(new WebPage("https://example.com", "", "<p>OOO 安卡拉</p>"), keywords);

        assertEquals(0, result.getCount(ankh.queryLabel()));
    }
}
