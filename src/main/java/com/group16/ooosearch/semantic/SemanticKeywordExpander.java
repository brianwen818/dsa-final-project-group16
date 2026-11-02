package com.group16.ooosearch.semantic;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.WebPage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SemanticKeywordExpander implements KeywordExpander {
    private final TfIdfAnalyzer analyzer;
    private final double relevanceThreshold;
    private final int maxIterations;
    private int currentIteration = 0;
    
    // 暫存最近一次擴充出來的新關鍵字（不含原關鍵字），方便 SearchResponse 拿取
    private List<Keyword> lastExpandedKeywords = new ArrayList<>();

    public SemanticKeywordExpander(Path stopwordsPath) {
        Tokenizer tokenizer = new Tokenizer(stopwordsPath);
        this.analyzer = new TfIdfAnalyzer(tokenizer);
        this.relevanceThreshold = 3.0; // 相關度門檻
        this.maxIterations = 1; // 限制迭代 1 次，防止 Topic Drift
    }

    @Override
    public KeywordList expand(List<WebPage> pages, KeywordList originalKeywords) {
        lastExpandedKeywords.clear();
        if (currentIteration >= maxIterations) {
            return originalKeywords;
        }
        currentIteration++;

        // 收集現有關鍵字及其所有同義寫法 (Forms)，避免重複擴充
        Set<String> knownForms = new HashSet<>();
        List<Keyword> originalList = originalKeywords.toList();
        for (Keyword kw : originalList) {
            knownForms.addAll(kw.getForms());
        }

        // 提取 Top-5 候選擴充詞
        List<TfIdfAnalyzer.CandidateWord> candidates = analyzer.extractTopK(pages, knownForms, 5);
        KeywordList expandedList = new KeywordList(originalList);

        // 篩選並加入符合門檻的新擴充詞（賦予權重 2.0）
        for (TfIdfAnalyzer.CandidateWord candidate : candidates) {
            String word = candidate.getWord();
            if (candidate.getScore() >= relevanceThreshold && !knownForms.contains(word)) {
                Keyword newKw = new Keyword(word, 2.0, List.of());
                expandedList.add(newKw);
                lastExpandedKeywords.add(newKw); // 收集純新擴充詞
                knownForms.add(word);
            }
        }

        return expandedList;
    }

    /** 專門提供給 SearchResponse 使用：取得不含原關鍵字的純擴充清單 */
    public List<Keyword> getLastExpandedKeywords() {
        return lastExpandedKeywords;
    }
}