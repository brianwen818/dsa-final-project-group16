package com.group16.ooosearch.semantic;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.WordDictionary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Tokenizer {
    private final JiebaSegmenter segmenter;
    private final Set<String> stopwords;

    public Tokenizer(Path stopwordsPath) {
        this.segmenter = new JiebaSegmenter();
        this.stopwords = loadStopwords(stopwordsPath);
    }

    // 載入自訂詞典 (可將 concepts.txt 或專用詞典加入 jieba 字典)
    public void loadCustomDictionary(Path customDictPath) {
        if (Files.exists(customDictPath)) {
            WordDictionary.getInstance().loadUserDict(customDictPath);
        }
    }

    private Set<String> loadStopwords(Path path) {
        Set<String> words = new HashSet<>();
        if (path != null && Files.exists(path)) {
            try {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        words.add(trimmed);
                    }
                }
            } catch (IOException e) {
                System.err.println("無法載入停用詞檔: " + e.getMessage());
            }
        }
        return words;
    }

    // 進行斷詞並過濾停用詞、過短字詞
    public List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        // 使用 jieba 進行搜尋模式斷詞
        List<String> rawTokens = segmenter.sentenceProcess(text);
        
        return rawTokens.stream()
                .map(String::trim)
                .filter(token -> token.length() > 1) // 過濾單字元噪音
                .filter(token -> !stopwords.contains(token)) // 過濾停用詞
                .filter(token -> !token.matches("^\\d+$")) // 過濾純數字
                .collect(Collectors.toList());
    }
}