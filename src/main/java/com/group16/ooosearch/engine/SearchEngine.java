package com.group16.ooosearch.engine;

/**
 * 整個搜尋引擎的總入口：串起 Stage 1–4。Stage 5 的網站只呼叫這個介面。
 * 修改此介面前，請先通知所有組員（README 5.1）。
 */
public interface SearchEngine {
    SearchResponse search(String query);
}
