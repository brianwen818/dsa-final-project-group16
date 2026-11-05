package com.group16.ooosearch.scoring;

import java.util.ArrayList;
import java.util.List;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;

/**
 * 計算查詢詞（含所有寫法）在一段文字中出現的次數。
 *
 * <p>計數規則與 Stage 1 相同（長的寫法優先、英文字界），並沿用兩種排除詞：
 * 關鍵字表中權重為 0 的詞（例如查「安卡」時不算「安卡拉」），
 * 以及查詢詞自己的排除詞（例如查「貓」時不算「熊貓」「貓頭鷹」，來自 data/query-synonyms.txt）。
 * 排除詞只佔住比對位置，不計次數。
 */
public class QueryCounter {
    private final Keyword query;
    private final List<Keyword> queryExclusions;
    private KeywordList cachedKeywords;
    private WordCounter cachedCounter;

    public QueryCounter(Keyword query, List<Keyword> queryExclusions) {
        this.query = query;
        this.queryExclusions = List.copyOf(queryExclusions);
    }

    public Keyword getQuery() {
        return query;
    }

    /** text 必須是已抽取的正文（不是原始 HTML）。keywords 只用來取出其中的排除詞。 */
    public int count(String text, KeywordList keywords) {
        return counterFor(keywords).count(text).get(query.getName());
    }

    private WordCounter counterFor(KeywordList keywords) {
        if (keywords != cachedKeywords || cachedCounter == null) {
            List<Keyword> list = new ArrayList<>();
            list.add(query);
            for (Keyword keyword : keywords) {
                if (keyword.getWeight() == 0 && !keyword.getName().equals(query.getName())) {
                    list.add(keyword);
                }
            }
            for (Keyword exclusion : queryExclusions) {
                if (!exclusion.getName().equals(query.getName())) {
                    list.add(new Keyword(exclusion.getName(), 0, exclusion.getAliases()));
                }
            }
            cachedKeywords = keywords;
            cachedCounter = new WordCounter(new KeywordList(list));
        }
        return cachedCounter;
    }
}
