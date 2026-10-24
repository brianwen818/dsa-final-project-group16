package com.group16.ooosearch.tree;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.PageResult;


public class RealSiteRanker implements SiteRanker {
    private final int maxDepth;
    private final int maxPagesPerTree;

    public RealSiteRanker(int maxDepth, int maxPagesPerTree) {
        this.maxDepth = maxDepth;
        this.maxPagesPerTree = maxPagesPerTree;
    }
    
    @Override
    public List<SiteResult> rank(List<String> rootUrls, KeywordList keywords) {
    List<SiteResult> results = new ArrayList<SiteResult>();
        
        // 1. 建立 Stage 2 的爬蟲（內部會重複使用 KeywordPageScorer 來快取同義詞表）
        SiteCrawler crawler = new SiteCrawler(maxDepth, maxPagesPerTree);

        for (String rootUrl : rootUrls) {
            // 2. 自動透過 BFS 爬蟲建立 WebTree
            WebTree tree = crawler.crawl(rootUrl);
            
            // 3. 透過後序走訪計算整棵樹的總分（套用 0.5 的深度衰減）
            double totalScore = tree.calculateTotalScores(0.5);
            
            // 4. 封裝成 SiteResult
            results.add(new SiteResult(rootUrl, totalScore, tree));
        }
        // 5. 依總分由高到低排序
        results.sort(Comparator.comparingDouble((SiteResult r) -> r.getTotalScore()).reversed());

        return results;
    }
}
