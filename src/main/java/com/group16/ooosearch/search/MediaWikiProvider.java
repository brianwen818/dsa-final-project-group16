package com.group16.ooosearch.search;

import com.group16.ooosearch.fetch.PageFetcher;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MediaWikiProvider implements SearchProvider {

    private final String host;
    private final PageFetcher fetcher;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public MediaWikiProvider(PageFetcher fetcher) {
        this("zh.wikipedia.org", fetcher);
    }

    public MediaWikiProvider(String host, PageFetcher fetcher) {
        this.host = host;
        this.fetcher = fetcher;
    }

    @Override
    public List<String> search(String query, int limit) {
        List<String> urls = new ArrayList<>();
        try {
            String apiUrl = "https://" + host + "/w/api.php?action=query&list=search&srsearch="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&format=json&formatversion=2";

            // 使用 PageFetcher 請求，享有快取與速率控管[cite: 3]
            String responseText = fetcher.fetchText(apiUrl);
            JsonNode root = jsonMapper.readTree(responseText);
            JsonNode searchResults = root.path("query").path("search");

            for (JsonNode node : searchResults) {
                if (urls.size() >= limit) break;
                String title = node.path("title").asText();
                String pageUrl = "https://" + host + "/wiki/" 
                        + URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8)
                                    .replace("+", "%20")
                                    .replace("%2F", "/");
                urls.add(pageUrl);
            }
        } catch (Exception e) {
            System.err.println("MediaWikiProvider 查詢失敗: " + e.getMessage());
        }
        return urls;
    }
}