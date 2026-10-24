package com.group16.ooosearch.tree;

import com.group16.ooosearch.fetch.MediaWikiClient;
import com.group16.ooosearch.fetch.PageCache;
import com.group16.ooosearch.fetch.PageFetcher;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.PageResult;
import com.group16.ooosearch.model.WebPage;
import com.group16.ooosearch.scoring.KeywordPageScorer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class SiteCrawler {
    private final KeywordPageScorer scorer;
    private final int maxDepth;
    private final int maxPagesPerTree;
    private final PageFetcher pageFetcher;

    public SiteCrawler(int maxDepth, int maxPagesPerTree) {
        this.scorer = new KeywordPageScorer();
        this.maxDepth = maxDepth;
        this.maxPagesPerTree = maxPagesPerTree;
        
        PageCache cache = new PageCache();
        this.pageFetcher = new PageFetcher(cache);
    }

    public WebTree crawl(String rootUrl) {
        KeywordList keywords;
        try {
            keywords = KeywordList.fromFile(Path.of("data", "keywords.txt"));
        } catch (Exception e) {
            System.err.println("無法讀取關鍵字檔案: " + e.getMessage());
            keywords = new KeywordList(); 
        }

        // 根節點初始化時帶入從 URL 解析的標題
        String rootTitle = extractTitleFromUrl(rootUrl);
        WebPage rootWebPage = new WebPage(rootUrl, rootTitle, "");
        WebTree tree = new WebTree(rootWebPage);
        WebNode rootNode = tree.getRoot();

        Set<String> visitedUrls = new HashSet<>();
        Queue<WebNode> queue = new LinkedList<>();

        queue.add(rootNode);
        visitedUrls.add(rootUrl);
        int crawledCount = 1;

        while (!queue.isEmpty() && crawledCount < maxPagesPerTree) {
            WebNode currentNode = queue.poll();
            String currentUrl = currentNode.getWebPage().getUrl();

            try {
                WebPage page = null;
                Document doc = null;

                // 1. 判斷是否為 MediaWiki 站點
                if (currentUrl.contains("wikipedia.org") || currentUrl.contains("fandom.com") || currentUrl.contains("kamenrider.huijiwiki.com") || currentUrl.contains("moegirl.org.cn")) {
                    String host = extractHost(currentUrl);
                    String title = extractWikiTitle(currentUrl);
                    
                    if (host != null && title != null && !title.isEmpty()) {
                        try {
                            MediaWikiClient mwClient = new MediaWikiClient(host, pageFetcher);
                            page = mwClient.parse(title, currentUrl.contains("zh.wikipedia.org") ? "zh-tw" : null);
                            doc = Jsoup.parse(page.getHtml(), currentUrl);
                        } catch (Exception apiEx) {
                            // API 失敗走備用邏輯
                        }
                    }
                }

                // 2. 如果不是 MediaWiki 或者是 API 抓取失敗，使用 Jsoup
                if (page == null || doc == null) {
                    String htmlContent = pageFetcher.fetchText(currentUrl);
                    doc = Jsoup.parse(htmlContent, currentUrl);
                    String title = doc.title();
                    if (title == null || title.trim().isEmpty()) {
                        title = extractTitleFromUrl(currentUrl);
                    }
                    page = new WebPage(currentUrl, title, htmlContent);
                }

                // 3. 進行 Stage 1 的關鍵字計分
                PageResult result = scorer.score(page, keywords);
                currentNode.setPageResult(result);

                // 4. 條件式剪枝與 BFS 擴展子連結
                if (currentNode.getDepth() < maxDepth && result.getScore() > 0.0) {
                    Elements links = doc.select("a[href]");
                    for (Element link : links) {
                        String absUrl = link.absUrl("href");

                        if (LinkFilter.isValidLink(rootUrl, absUrl) && !visitedUrls.contains(absUrl)) {
                            visitedUrls.add(absUrl);
                            
                            try {   
                                // 為了讓子節點也有分數與關鍵字次數，我們需要抓取子頁面的內容
                                WebPage childPage = null;
                                Document childDoc = null;
                                
                                // 判斷子頁面是否也適用 MediaWiki API
                                if (absUrl.contains("wikipedia.org") || absUrl.contains("moegirl.org.cn")) {
                                    String host = extractHost(absUrl);
                                    String wikiTitle = extractWikiTitle(absUrl);
                                    if (host != null && wikiTitle != null && !wikiTitle.isEmpty()) {
                                        try {
                                            MediaWikiClient mwClient = new MediaWikiClient(host, pageFetcher);
                                            childPage = mwClient.parse(wikiTitle, absUrl.contains("zh.wikipedia.org") ? "zh-tw" : null);
                                            childDoc = Jsoup.parse(childPage.getHtml(), absUrl);
                                        } catch (Exception ignored) {}
                                    }
                                }
                                
                                // 如果不是 Wiki 或是 API 失敗，用 Jsoup 抓取
                                if (childPage == null || childDoc == null) {
                                    String childHtml = pageFetcher.fetchText(absUrl);
                                    childDoc = Jsoup.parse(childHtml, absUrl);
                                    String childTitle = childDoc.title();
                                    if (childTitle == null || childTitle.trim().isEmpty()) {
                                        childTitle = extractTitleFromUrl(absUrl);
                                    }
                                    childPage = new WebPage(absUrl, childTitle, childHtml);
                                }

                                // 對子頁面進行 Stage 1 計分
                                PageResult childResult = scorer.score(childPage, keywords);

                                WebNode childNode = new WebNode(childPage);
                                childNode.setPageResult(childResult); // 賦予計分結果
                                childNode.setDepth(currentNode.getDepth() + 1);
                                currentNode.addChild(childNode);
                                
                                queue.add(childNode);
                                crawledCount++;

                                if (crawledCount >= maxPagesPerTree) break;
                                } catch (Exception childEx) {
                                // 子頁面抓取失敗則略過
                                }
                            }
                        }
                    }

                     
            } catch (Exception e) {
                System.err.println("Failed to crawl: " + currentUrl + " (" + e.getMessage() + ")");
            }
        }

        return tree;
    }

    private String extractHost(String urlStr) {
        try {
            java.net.URI uri = new java.net.URI(urlStr);
            return uri.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractWikiTitle(String urlStr) {
        try {
            int wikiIndex = urlStr.indexOf("/wiki/");
            if (wikiIndex != -1) {
                String rawTitle = urlStr.substring(wikiIndex + 6);
                return java.net.URLDecoder.decode(rawTitle, java.nio.charset.StandardCharsets.UTF_8);
            }
            int zhIndex = urlStr.indexOf("/zh-tw/");
            if (zhIndex != -1) {
                String rawTitle = urlStr.substring(zhIndex + 7);
                return java.net.URLDecoder.decode(rawTitle, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String extractTitleFromUrl(String urlStr) {
        try {
            int lastSlash = urlStr.lastIndexOf('/');
            if (lastSlash != -1 && lastSlash < urlStr.length() - 1) {
                String rawName = urlStr.substring(lastSlash + 1);
                // 透過明確指定 UTF-8 進行解碼，避免系統預設編碼造成亂碼或問號
                return java.net.URLDecoder.decode(rawName, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            // ignore
        }
        return urlStr;
    }

}