package com.group16.ooosearch.fetch;

import java.io.IOException;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.group16.ooosearch.model.WebPage;

/**
 * 抓取網頁。先查 PageCache，沒有快取才連線下載，下載後存入快取。
 * 兩次連線之間至少間隔 minIntervalMillis，避免被網站封鎖。
 */
public class PageFetcher {
    /** 依 Wikimedia 的 User-Agent 規範，附上專案網址作為聯絡方式。 */
    private static final String USER_AGENT =
            "Group16-OOO-Search/0.1 (https://github.com/brianwen818/Data-Structure-Final-Project---Group-16; course project)";

    private final PageCache cache;
    private final long minIntervalMillis;
    private final int timeoutMillis;
    private long lastRequestTime = 0;

    public PageFetcher(PageCache cache) {
        this(cache, 1000, 10000);
    }

    public PageFetcher(PageCache cache, long minIntervalMillis, int timeoutMillis) {
        this.cache = cache;
        this.minIntervalMillis = minIntervalMillis;
        this.timeoutMillis = timeoutMillis;
    }

    public WebPage fetch(String url) throws IOException {
        String html = fetchText(url);
        Document doc = Jsoup.parse(html, url);
        return new WebPage(url, doc.title(), html);
    }

    /** 取得網址回傳的原始內容（HTML、JSON 等），一樣會使用快取。 */
    public String fetchText(String url) throws IOException {
        Optional<String> cached = cache.get(url);
        return cached.isPresent() ? cached.get() : download(url);
    }

    private String download(String url) throws IOException {
        waitForInterval();
        String body = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(timeoutMillis)
                .ignoreContentType(true)
                .maxBodySize(0)
                .execute()
                .body();
        cache.put(url, body);
        return body;
    }

    private void waitForInterval() throws IOException {
        long wait = lastRequestTime + minIntervalMillis - System.currentTimeMillis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("等待下一次連線時被中斷", e);
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }
}
