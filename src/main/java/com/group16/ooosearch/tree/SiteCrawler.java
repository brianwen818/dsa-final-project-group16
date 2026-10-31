package com.group16.ooosearch.tree;

import com.group16.ooosearch.fetch.MediaWikiClient;
import com.group16.ooosearch.fetch.PageCache;
import com.group16.ooosearch.fetch.PageFetcher;
import com.group16.ooosearch.fetch.UrlCanonicalizer;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.WebPage;
import com.group16.ooosearch.scoring.KeywordPageScorer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class SiteCrawler {
    /** 每個節點最多展開幾個子頁面，避免額度在第 1 層就用完，讓樹能長到更深。 */
    private static final int DEFAULT_MAX_CHILDREN_PER_NODE = 5;
    /** 維護模板、導覽框、編輯連結、參考資料等區塊的連結不算正文連結。 */
    private static final String NON_CONTENT_BLOCKS =
            "nav, header, footer, aside, .ambox, .navbox, .metadata, .hatnote, .catlinks, "
            + ".mw-editsection, .reference, .references, .reflist";

    private final KeywordPageScorer scorer;
    private final int maxDepth;
    private final int maxPagesPerTree;
    private final int maxChildrenPerNode;
    private final PageFetcher pageFetcher;

    public SiteCrawler(int maxDepth, int maxPagesPerTree) {
        this(maxDepth, maxPagesPerTree, DEFAULT_MAX_CHILDREN_PER_NODE, new PageFetcher(new PageCache()));
    }

    public SiteCrawler(int maxDepth, int maxPagesPerTree, int maxChildrenPerNode, PageFetcher pageFetcher) {
        this.scorer = new KeywordPageScorer();
        this.maxDepth = maxDepth;
        this.maxPagesPerTree = maxPagesPerTree;
        this.maxChildrenPerNode = maxChildrenPerNode;
        this.pageFetcher = pageFetcher;
    }

    /** 根網址抓取失敗時，回傳只有根節點、getPageResult() 為 null 的樹，代表無法評分。 */
    public WebTree crawl(String rootUrl, KeywordList keywords) {
        WebPage rootPage;
        try {
            rootPage = loadPage(rootUrl);
        } catch (Exception e) {
            System.err.println("Failed to crawl: " + rootUrl + " (" + e.getMessage() + ")");
            return new WebTree(new WebPage(rootUrl, extractTitleFromUrl(rootUrl), ""));
        }

        WebTree tree = new WebTree(rootPage);
        WebNode rootNode = tree.getRoot();
        rootNode.setPageResult(scorer.score(rootPage, keywords));

        Set<String> visitedUrls = new HashSet<>();
        visitedUrls.add(UrlCanonicalizer.canonicalize(rootUrl));
        Queue<WebNode> queue = new LinkedList<>();
        queue.add(rootNode);
        int crawledCount = 1;

        while (!queue.isEmpty() && crawledCount < maxPagesPerTree) {
            WebNode currentNode = queue.poll();
            // 條件式剪枝：超過深度或 0 分的頁面不再往下展開
            if (currentNode.getDepth() >= maxDepth || currentNode.getPageResult().getScore() <= 0.0) {
                continue;
            }

            Document doc = Jsoup.parse(currentNode.getWebPage().getHtml(), currentNode.getWebPage().getUrl());
            doc.select(NON_CONTENT_BLOCKS).remove();

            int childCount = 0;
            for (Element link : doc.select("a[href]")) {
                String absUrl = link.absUrl("href");
                if (!LinkFilter.isValidLink(rootUrl, absUrl) || !visitedUrls.add(UrlCanonicalizer.canonicalize(absUrl))) {
                    continue;
                }
                try {
                    WebPage childPage = loadPage(absUrl);
                    WebNode childNode = new WebNode(childPage);
                    childNode.setPageResult(scorer.score(childPage, keywords));
                    currentNode.addChild(childNode);
                    queue.add(childNode);
                    crawledCount++;
                    childCount++;
                } catch (Exception childEx) {
                    // 子頁面抓取失敗則略過
                }
                if (crawledCount >= maxPagesPerTree || childCount >= maxChildrenPerNode) {
                    break;
                }
            }
        }

        return tree;
    }

    /** MediaWiki 站點優先用 API（可避開前端渲染與反爬蟲），失敗或其他網站則直接抓 HTML。 */
    private WebPage loadPage(String url) throws IOException {
        if (isMediaWikiSite(url)) {
            String host = extractHost(url);
            String title = extractWikiTitle(url);
            if (host != null && title != null && !title.isEmpty()) {
                try {
                    return new MediaWikiClient(host, pageFetcher)
                            .parse(title, url.contains("zh.wikipedia.org") ? "zh-tw" : null);
                } catch (Exception apiEx) {
                    // API 失敗走備用邏輯
                }
            }
        }
        String html = pageFetcher.fetchText(url);
        String title = Jsoup.parse(html, url).title();
        if (title == null || title.trim().isEmpty()) {
            title = extractTitleFromUrl(url);
        }
        return new WebPage(url, title, html);
    }

    private boolean isMediaWikiSite(String url) {
        return url.contains("wikipedia.org") || url.contains("fandom.com")
                || url.contains("kamenrider.huijiwiki.com") || url.contains("moegirl.org.cn");
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