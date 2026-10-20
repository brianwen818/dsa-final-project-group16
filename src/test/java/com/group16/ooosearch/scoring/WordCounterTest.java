package com.group16.ooosearch.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;

class WordCounterTest {

    private final KeywordList keywords = new KeywordList(List.of(
            new Keyword("假面騎士OOO", 5, List.of("仮面ライダーオーズ", "オーズ", "OOO", "Kamen Rider OOO")),
            new Keyword("安酷", 4, List.of("安卡", "アンク", "Ankh")),
            new Keyword("火野映司", 4, List.of("映司")),
            new Keyword("假面騎士Birth", 3, List.of("Kamen Rider Birth")),
            new Keyword("安卡拉", 0)));
    private final WordCounter counter = new WordCounter(keywords);

    @Test
    void longFormIsNotCountedAgainAsShortForm() {
        Map<String, Integer> counts = counter.count("假面騎士OOO 是 OOO 系列");
        assertEquals(2, counts.get("假面騎士OOO"));
    }

    @Test
    void synonymsAreAddedToTheirKeyword() {
        Map<String, Integer> counts = counter.count("安酷、安卡、アンク、Ankh");
        assertEquals(4, counts.get("安酷"));
    }

    @Test
    void exclusionWordBlocksShorterForm() {
        Map<String, Integer> counts = counter.count("安卡拉是土耳其首都，安卡出現一次");
        assertEquals(1, counts.get("安卡拉"));
        assertEquals(1, counts.get("安酷"));
    }

    @Test
    void ignoresCaseAndFullWidth() {
        Map<String, Integer> counts = counter.count("kamen rider ooo、ＯＯＯ、ANKH");
        assertEquals(2, counts.get("假面騎士OOO"));
        assertEquals(1, counts.get("安酷"));
    }

    @Test
    void englishFormsNeedWordBoundaries() {
        Map<String, Integer> counts = counter.count("Ankhs OOOO Kamen Rider Birthday");
        assertEquals(0, counts.get("安酷"));
        assertEquals(0, counts.get("假面騎士OOO"));
        assertEquals(0, counts.get("假面騎士Birth"));
    }

    @Test
    void cjkFormsDoNotNeedWordBoundaries() {
        // 中日文沒有空白分詞，前後接著其他字也要算
        assertEquals(1, counter.count("主角火野映司登場").get("火野映司"));
        assertEquals(1, counter.count("オーズドライバー").get("假面騎士OOO"));
    }

    @Test
    void ignoresSpacesBetweenCjkCharacters() {
        assertEquals(1, counter.count("火野 映司").get("火野映司"));
    }

    @Test
    void countsZeroForMissingKeywords() {
        Map<String, Integer> counts = counter.count("沒有任何關鍵字");
        assertEquals(5, counts.size());
        assertEquals(0, counts.get("火野映司"));
    }

    @Test
    void rejectsFormBelongingToTwoKeywords() {
        KeywordList conflicting = new KeywordList(List.of(
                new Keyword("A", 1, List.of("同一個")), new Keyword("B", 1, List.of("同一個"))));
        assertThrows(IllegalArgumentException.class, () -> new WordCounter(conflicting));
    }
}
