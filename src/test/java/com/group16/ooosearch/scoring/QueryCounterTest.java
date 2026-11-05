package com.group16.ooosearch.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;

class QueryCounterTest {
    private final KeywordList keywords = new KeywordList(List.of(
            new Keyword("假面騎士OOO", 5, List.of("OOO")),
            new Keyword("安卡拉", 0)));

    @Test
    void countsAllFormsButSkipsBothKindsOfExclusions() {
        QueryCounter cat = new QueryCounter(new Keyword("貓咪", 1, List.of("貓")),
                List.of(new Keyword("熊貓", 0), new Keyword("貓頭鷹", 0)));

        assertEquals(3, cat.count("貓咪與貓，還有貓系聯組。熊貓不算，貓頭鷹也不算。", keywords));

        QueryCounter ankh = new QueryCounter(new Keyword("安卡", 1), List.of());
        assertEquals(1, ankh.count("安卡與安卡拉", keywords)); // 關鍵字表的排除詞
    }
}
