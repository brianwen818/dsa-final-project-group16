package com.group16.ooosearch.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.PageResult;
import com.group16.ooosearch.model.WebPage;

class KeywordPageScorerTest {

    private final KeywordList keywords = new KeywordList(List.of(
            new Keyword("假面騎士OOO", 5, List.of("OOO")),
            new Keyword("安酷", 4, List.of("安卡")),
            new Keyword("安卡拉", 0)));
    private final KeywordPageScorer scorer = new KeywordPageScorer();

    @Test
    void scoreIsWeightTimesCountOfBodyText() {
        String html = "<html><head><title>OOO OOO OOO</title><script>var s = '安酷';</script></head>"
                + "<body><nav>OOO</nav><p>假面騎士OOO 的主角之一是安酷，又譯安卡。</p></body></html>";
        PageResult result = scorer.score(new WebPage("https://example.com", "", html), keywords);

        assertEquals(1, result.getCount("假面騎士OOO"));
        assertEquals(2, result.getCount("安酷"));
        assertEquals(5 * 1 + 4 * 2, result.getScore());
    }

    @Test
    void exclusionWordsAreNotListedAndAddNothing() {
        PageResult result = scorer.score(new WebPage("https://example.com", "", "<p>安卡拉</p>"), keywords);

        assertEquals(0.0, result.getScore());
        assertFalse(result.getKeywordCounts().containsKey("安卡拉"));
        assertEquals(0, result.getCount("安酷"));
    }
}
