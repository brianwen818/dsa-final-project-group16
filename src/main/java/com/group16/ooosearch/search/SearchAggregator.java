package com.group16.ooosearch.search;

import com.group16.ooosearch.fetch.UrlCanonicalizer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SearchAggregator 負責聚合多個 SearchProvider 的搜尋結果，
 * 先把網址正規化，再透過 LinkedHashSet 進行自動去重與保留順序。
 */
public class SearchAggregator {

    private final List<SearchProvider> providers;

    public SearchAggregator(List<SearchProvider> providers) {
        this.providers = providers;
    }

    /**
     * 對所有已註冊的 Provider 發送搜尋請求，並整合去重後的網址清單
     *
     * @param query 關鍵字
     * @param limitPerProvider 每個 Provider 最多抓取的網址數量
     * @return 去重後的 URL 清單
     */
    public List<String> search(String query, int limitPerProvider) {
        // 使用 LinkedHashSet 確保插入順序，同時自動過濾重複的 URL
        Set<String> uniqueUrls = new LinkedHashSet<>();

        for (SearchProvider provider : providers) {
            try {
                List<String> results = provider.search(query, limitPerProvider);
                if (results != null) {
                    for (String url : results) {
                        uniqueUrls.add(UrlCanonicalizer.canonicalize(url));
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ Provider [" + provider.getClass().getSimpleName() + "] 搜尋失敗: " + e.getMessage());
            }
        }

        return new ArrayList<>(uniqueUrls);
    }
}