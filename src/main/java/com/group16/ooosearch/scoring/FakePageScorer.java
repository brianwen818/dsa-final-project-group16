package com.group16.ooosearch.scoring;

import java.util.LinkedHashMap;
import java.util.Map;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.PageResult;
import com.group16.ooosearch.model.WebPage;

/**
 * 假實作：每頁都是 10 分，每個關鍵字次數為 0。
 * 讓 Stage 2 在 Stage 1 完成前也能開發與測試。
 */
public class FakePageScorer implements PageScorer {
    public static final double FAKE_SCORE = 10.0;

    @Override
    public PageResult score(WebPage page, KeywordList keywords) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Keyword keyword : keywords) {
            counts.put(keyword.getName(), 0);
        }
        return new PageResult(page, FAKE_SCORE, counts);
    }
}
