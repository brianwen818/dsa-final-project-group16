package com.group16.ooosearch.search;

import java.util.List;

/**
 * Stage 3 提供：一個搜尋來源，給查詢字串，回傳最多 limit 個候選網址。
 * 各來源（DuckDuckGo、MediaWiki、Google 快取…）各寫一個實作。
 * 修改此介面前，請先通知所有組員（README 5.1）。
 */
public interface SearchProvider {
    List<String> search(String query, int limit);
}
