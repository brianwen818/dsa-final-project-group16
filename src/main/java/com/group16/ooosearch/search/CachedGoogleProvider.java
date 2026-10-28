package com.group16.ooosearch.search;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CachedGoogleProvider implements SearchProvider {

    private final Path jsonPath;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public CachedGoogleProvider() {
        this(Path.of("data", "google_results.json"));
    }

    public CachedGoogleProvider(Path jsonPath) {
        this.jsonPath = jsonPath;
    }

    @Override
    public List<String> search(String query, int limit) {
        List<String> urls = new ArrayList<>();

        if (!Files.exists(jsonPath)) {
            System.err.println("⚠️ 找不到 Google 快取檔案: " + jsonPath.toAbsolutePath());
            return urls;
        }

        try {
            JsonNode root = jsonMapper.readTree(jsonPath.toFile());
            JsonNode queryNode = root.get(query);

            if (queryNode != null) {
                collectUrls(queryNode, urls, limit);
            }
        } catch (Exception e) {
            System.err.println("CachedGoogleProvider 讀取失敗: " + e.getMessage());
        }

        return urls;
    }

    private void collectUrls(JsonNode node, List<String> urls, int limit) {
        if (urls.size() >= limit || node == null) return;

        if (node.isArray()) {
            for (JsonNode item : node) {
                if (urls.size() >= limit) break;
                collectUrls(item, urls, limit);
            }
        } else if (node.isObject()) {
            // 嘗試從常見的 URL 欄位擷取字串
            String foundUrl = null;
            for (String fieldName : new String[]{"url", "link", "href", "cite"}) {
                JsonNode valNode = node.get(fieldName);
                if (valNode != null && valNode.isValueNode()) {
                    String str = valNode.asString();
                    if (str.startsWith("http://") || str.startsWith("https://")) {
                        foundUrl = str;
                        break;
                    }
                }
            }

            if (foundUrl != null) {
                urls.add(foundUrl);
            } else {
                // 若此物件無 URL 欄位，繼續遍歷子節點
                for (String propertyName : node.propertyNames()) {
                    if (urls.size() >= limit) break;
                    collectUrls(node.get(propertyName), urls, limit);
                }
            }
        } else if (node.isValueNode()) {
            String str = node.asString();
            if (str.startsWith("http://") || str.startsWith("https://")) {
                urls.add(str);
            }
        }
    }
}