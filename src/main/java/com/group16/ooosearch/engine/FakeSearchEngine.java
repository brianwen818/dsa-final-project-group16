package com.group16.ooosearch.engine;

import java.util.List;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.search.FakeSearchProvider;
import com.group16.ooosearch.search.SearchProvider;
import com.group16.ooosearch.tree.FakeSiteRanker;
import com.group16.ooosearch.tree.SiteRanker;
import com.group16.ooosearch.tree.SiteResult;

/**
 * 假實作：不連網路，用其他假實作組出一份固定格式的結果，讓 Stage 5 能先做前端。
 * 查詢字串以空白切開，每個詞當作權重 1 的關鍵字。
 */
public class FakeSearchEngine implements SearchEngine {
    private static final int RESULT_LIMIT = 5;

    private final SearchProvider provider = new FakeSearchProvider();
    private final SiteRanker ranker = new FakeSiteRanker();

    @Override
    public SearchResponse search(String query) {
        KeywordList keywords = new KeywordList();
        for (String word : query.strip().split("\\s+")) {
            if (!word.isEmpty()) {
                keywords.add(new Keyword(word, 1.0));
            }
        }
        List<String> urls = provider.search(query, RESULT_LIMIT);
        List<SiteResult> results = ranker.rank(urls, keywords);
        List<Keyword> expanded = List.of(new Keyword("塔托巴組合", 0.5), new Keyword("鴻上集團", 0.5));
        return new SearchResponse(query, keywords, results, expanded);
    }
}
