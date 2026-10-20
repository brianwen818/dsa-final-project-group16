package com.group16.ooosearch.semantic;

import java.util.List;

import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.WebPage;

/**
 * Stage 4 提供：從抓回的網頁找出相關詞，回傳「原關鍵字 + 擴充關鍵字」。
 * 擴充出的關鍵字權重應低於原關鍵字。
 * 修改此介面前，請先通知所有組員（README 5.1）。
 */
public interface KeywordExpander {
    KeywordList expand(List<WebPage> pages, KeywordList original);
}
