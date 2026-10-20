package com.group16.ooosearch.tree;

import java.util.List;

import com.group16.ooosearch.model.KeywordList;

/**
 * Stage 2 提供：爬取每個根網址的子網頁並建樹，回傳依總分由高到低排序的結果。
 * 修改此介面前，請先通知所有組員（README 5.1）。
 */
public interface SiteRanker {
    List<SiteResult> rank(List<String> rootUrls, KeywordList keywords);
}
