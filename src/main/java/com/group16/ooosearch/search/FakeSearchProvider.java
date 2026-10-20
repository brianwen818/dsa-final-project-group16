package com.group16.ooosearch.search;

import java.util.ArrayList;
import java.util.List;

/**
 * 假實作：不連網路，回傳固定的範例網址（最多 5 個）。
 */
public class FakeSearchProvider implements SearchProvider {
    private static final int MAX_RESULTS = 5;

    @Override
    public List<String> search(String query, int limit) {
        List<String> urls = new ArrayList<>();
        for (int i = 1; i <= Math.min(limit, MAX_RESULTS); i++) {
            urls.add("https://example.com/ooo/" + i);
        }
        return urls;
    }
}
