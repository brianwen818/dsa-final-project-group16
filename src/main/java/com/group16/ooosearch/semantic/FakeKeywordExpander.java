package com.group16.ooosearch.semantic;

import java.util.List;

import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.WebPage;

/**
 * 假實作：不擴充，原封不動回傳原關鍵字。
 */
public class FakeKeywordExpander implements KeywordExpander {
    @Override
    public KeywordList expand(List<WebPage> pages, KeywordList original) {
        return new KeywordList(original.toList());
    }
}
