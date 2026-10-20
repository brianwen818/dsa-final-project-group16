package com.group16.ooosearch.tree;

import java.util.ArrayList;
import java.util.List;

import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.PageResult;
import com.group16.ooosearch.model.WebPage;
import com.group16.ooosearch.scoring.FakePageScorer;

/**
 * 假實作：不連網路，每個根網址建一個單節點的樹，分數 10，依輸入順序排名。
 * 讓 Stage 3、5 在 Stage 2 完成前也能開發與測試。
 */
public class FakeSiteRanker implements SiteRanker {
    private final FakePageScorer scorer = new FakePageScorer();

    @Override
    public List<SiteResult> rank(List<String> rootUrls, KeywordList keywords) {
        List<SiteResult> results = new ArrayList<>();
        for (int i = 0; i < rootUrls.size(); i++) {
            String url = rootUrls.get(i);
            WebTree tree = new WebTree(new WebPage(url, "範例網站 " + (i + 1), ""));
            PageResult pageResult = scorer.score(tree.getRoot().getWebPage(), keywords);
            tree.getRoot().setPageResult(pageResult);
            tree.getRoot().setNodeScore(pageResult.getScore());
            results.add(new SiteResult(url, pageResult.getScore(), tree));
        }
        return results;
    }
}
