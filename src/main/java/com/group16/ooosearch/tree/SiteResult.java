package com.group16.ooosearch.tree;

/**
 * 一個網站的計分結果（Stage 2 的輸出）：根網址、總分與子網頁樹。
 * 名次就是它在 SiteRanker 回傳清單中的位置。
 */
public class SiteResult {
    private final String rootUrl;
    private final double totalScore;
    private final WebTree tree;

    public SiteResult(String rootUrl, double totalScore, WebTree tree) {
        this.rootUrl = rootUrl;
        this.totalScore = totalScore;
        this.tree = tree;
    }

    public String getRootUrl() {
        return rootUrl;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public WebTree getTree() {
        return tree;
    }
}
