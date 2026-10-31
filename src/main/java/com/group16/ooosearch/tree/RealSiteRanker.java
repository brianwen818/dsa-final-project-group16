package com.group16.ooosearch.tree;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import com.group16.ooosearch.model.KeywordList;


public class RealSiteRanker implements SiteRanker {
    private final SiteCrawler crawler;

    public RealSiteRanker(int maxDepth, int maxPagesPerTree) {
        this(new SiteCrawler(maxDepth, maxPagesPerTree));
    }

    public RealSiteRanker(SiteCrawler crawler) {
        this.crawler = crawler;
    }
    
    @Override
    public List<SiteResult> rank(List<String> rootUrls, KeywordList keywords) {
    List<SiteResult> results = new ArrayList<SiteResult>();
        
        for (String rootUrl : rootUrls) {
            // 1. 自動透過 BFS 爬蟲建立 WebTree
            WebTree tree = crawler.crawl(rootUrl, keywords);
            
            // 2. 透過後序走訪計算整棵樹的總分（套用 0.5 的深度衰減）
            double totalScore = tree.calculateTotalScores(0.5);
            
            // 3. 封裝成 SiteResult
            results.add(new SiteResult(rootUrl, totalScore, tree));
        }
        // 4. 依總分由高到低排序
        results.sort(Comparator.comparingDouble((SiteResult r) -> r.getTotalScore()).reversed());

        return results;
    }
}
