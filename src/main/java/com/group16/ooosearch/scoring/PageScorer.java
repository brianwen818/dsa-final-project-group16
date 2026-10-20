package com.group16.ooosearch.scoring;

import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.PageResult;
import com.group16.ooosearch.model.WebPage;

/**
 * Stage 1 提供：計算單一網頁的分數與各關鍵字出現次數。
 * 修改此介面前，請先通知所有組員（README 5.1）。
 */
public interface PageScorer {
    PageResult score(WebPage page, KeywordList keywords);
}
