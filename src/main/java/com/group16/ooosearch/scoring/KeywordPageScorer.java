package com.group16.ooosearch.scoring;

import java.util.LinkedHashMap;
import java.util.Map;

import com.group16.ooosearch.fetch.TextExtractor;
import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.PageResult;
import com.group16.ooosearch.model.WebPage;

/**
 * PageScorer 的正式實作（Stage 1）：
 * 抽取正文 → 計算每個關鍵字（含同義詞）的次數 → 分數 = Σ(權重 × 次數)。
 * 權重為 0 的關鍵字是排除詞，只用來佔住比對位置，不列入 PageResult。
 */
public class KeywordPageScorer implements PageScorer {
    private final TextExtractor extractor = new TextExtractor();
    private KeywordList cachedKeywords;
    private WordCounter cachedCounter;

    @Override
    public PageResult score(WebPage page, KeywordList keywords) {
        Map<String, Integer> counts = counterFor(keywords).count(extractor.extract(page.getHtml()));
        double score = 0;
        Map<String, Integer> shown = new LinkedHashMap<>();
        for (Keyword keyword : keywords) {
            int count = counts.get(keyword.getName());
            score += keyword.getWeight() * count;
            if (keyword.getWeight() > 0) {
                shown.put(keyword.getName(), count);
            }
        }
        return new PageResult(page, score, shown);
    }

    /** 同一組關鍵字重複使用同一個 WordCounter，不必每頁都重建同義詞表。 */
    private WordCounter counterFor(KeywordList keywords) {
        if (keywords != cachedKeywords) {
            cachedKeywords = keywords;
            cachedCounter = new WordCounter(keywords);
        }
        return cachedCounter;
    }
}
