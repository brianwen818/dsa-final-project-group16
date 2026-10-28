package com.group16.ooosearch.search;

import com.group16.ooosearch.fetch.PageFetcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DuckDuckGoProvider implements SearchProvider {

    private final PageFetcher fetcher;

    public DuckDuckGoProvider(PageFetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public List<String> search(String query, int limit) {
        List<String> urls = new ArrayList<>();
        try {
            String searchUrl = "https://html.duckduckgo.com/html/?q=" 
                    + URLEncoder.encode(query, StandardCharsets.UTF_8);

            // 透過 PageFetcher 抓取 HTML[cite: 3]
            String html = fetcher.fetchText(searchUrl);
            Document doc = Jsoup.parse(html, searchUrl);

            Elements links = doc.select("a.result__a");
            for (Element link : links) {
                if (urls.size() >= limit) break;
                String href = link.attr("href");

                // 解碼 DuckDuckGo 轉址網址 (uddg=https%3A%2F%2F...)
                if (href.contains("uddg=")) {
                    int uddgIndex = href.indexOf("uddg=");
                    String rawUrl = href.substring(uddgIndex + 5);
                    int ampIndex = rawUrl.indexOf("&");
                    if (ampIndex != -1) {
                        rawUrl = rawUrl.substring(0, ampIndex);
                    }
                    String realUrl = URLDecoder.decode(rawUrl, StandardCharsets.UTF_8);
                    urls.add(realUrl);
                } else if (href.startsWith("http")) {
                    urls.add(href);
                }
            }
        } catch (Exception e) {
            System.err.println("DuckDuckGoProvider 查詢失敗: " + e.getMessage());
        }
        return urls;
    }
}