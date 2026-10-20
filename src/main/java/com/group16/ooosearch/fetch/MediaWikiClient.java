package com.group16.ooosearch.fetch;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.group16.ooosearch.model.WebPage;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * MediaWiki API（維基百科、Fandom 等）的簡易用戶端。
 * 所有請求都透過 PageFetcher，因此會使用快取並控制連線頻率。
 */
public class MediaWikiClient {
    private final String host;
    private final PageFetcher fetcher;
    private final JsonMapper json = JsonMapper.builder().build();

    /** host 例如 "ja.wikipedia.org"、"zh.wikipedia.org"。 */
    public MediaWikiClient(String host, PageFetcher fetcher) {
        this.host = host;
        this.fetcher = fetcher;
    }

    /**
     * 取得條目解析後的 HTML（會跟隨重新導向）。
     * variant 是中文維基的字詞轉換，例如 "zh-tw"（台灣正體）；不需要時傳 null。
     */
    public WebPage parse(String title, String variant) throws IOException {
        String url = apiUrl("action=parse&prop=text&redirects=1&page=" + encode(title))
                + (variant == null ? "" : "&variant=" + variant);
        JsonNode parse = request(url, title).get("parse");
        String resolvedTitle = parse.get("title").asString();
        String html = parse.get("text").asString();
        return new WebPage(pageUrl(resolvedTitle), resolvedTitle, html);
    }

    /**
     * 查詢各條目在另一個語言版本的標題（跨語言連結），例如 lang = "zh"。
     * 回傳「原標題 → 另一語言標題」；沒有對應條目的標題不會出現在結果中。
     */
    public Map<String, String> langLinks(List<String> titles, String lang) throws IOException {
        String url = apiUrl("action=query&prop=langlinks&redirects=1&lllimit=max&lllang=" + lang
                + "&titles=" + encode(String.join("|", titles)));
        JsonNode query = request(url, String.join("|", titles)).get("query");

        // 原標題可能被重新導向，先記下「導向後標題 → 原標題」
        Map<String, String> originalOf = new HashMap<>();
        for (String title : titles) {
            originalOf.put(title, title);
        }
        for (JsonNode redirect : query.path("redirects")) {
            originalOf.put(redirect.get("to").asString(), redirect.get("from").asString());
        }

        Map<String, String> result = new LinkedHashMap<>();
        for (JsonNode page : query.path("pages")) {
            JsonNode links = page.path("langlinks");
            if (links.isEmpty()) {
                continue;
            }
            String title = page.get("title").asString();
            result.put(originalOf.getOrDefault(title, title), links.get(0).get("title").asString());
        }
        return result;
    }

    public String pageUrl(String title) {
        return "https://" + host + "/wiki/" + encode(title.replace(' ', '_')).replace("+", "%20").replace("%2F", "/");
    }

    private JsonNode request(String url, String what) throws IOException {
        JsonNode root = json.readTree(fetcher.fetchText(url));
        if (root.has("error")) {
            throw new IOException(host + " 查詢「" + what + "」失敗：" + root.get("error").path("info").asString());
        }
        return root;
    }

    private String apiUrl(String params) {
        return "https://" + host + "/w/api.php?format=json&formatversion=2&" + params;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
