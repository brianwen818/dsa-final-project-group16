package com.group16.ooosearch.semantic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import com.group16.ooosearch.fetch.TextExtractor;
import com.group16.ooosearch.model.WebPage;

public class TfIdfAnalyzer {
    private final Tokenizer tokenizer;
    private final TextExtractor textExtractor; // 建立 TextExtractor 實例

    public TfIdfAnalyzer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.textExtractor = new TextExtractor(); // 初始化
    }

    public static class CandidateWord {
        private final String word;
        private final double score;

        public CandidateWord(String word, double score) {
            this.word = word;
            this.score = score;
        }

        public String getWord() { return word; }
        public double getScore() { return score; }
    }

    /**
     * 建構共現關係 Graph（鄰接表），並計算分數取出 Top-K 擴充詞（DF 以 pages 本身計算）。
     */
    public List<CandidateWord> extractTopK(List<WebPage> pages, Set<String> originalKeywords, int topK) {
        return extractTopK(pages, originalKeywords, topK, List.of());
    }

    /**
     * 同上，但 DF（文件頻率）改用 background 這批網頁計算（例如所有爬到的網頁）。
     * pages 只是少數相關段落時，「使用」「發出」這類泛用詞在段落中的 DF 很低、壓不下來；
     * 改用整批網頁當背景，泛用詞的 DF 高、IDF 低，只在少數網頁出現的專有名詞才會勝出。
     */
    public List<CandidateWord> extractTopK(List<WebPage> pages, Set<String> originalKeywords, int topK,
                                           List<WebPage> background) {
        // 1. 建立鄰接表 Graph：Map<WordA, Map<WordB, 共現次數>>
        Map<String, Map<String, Integer>> adjacencyList = new HashMap<>();
        Map<String, Integer> documentFrequency = new HashMap<>();
        boolean useBackground = !background.isEmpty();
        int totalDocs = useBackground ? background.size() : pages.size();
        if (useBackground) {
            for (WebPage page : background) {
                for (String token : new HashSet<>(tokenizer.tokenize(textExtractor.extract(page.getHtml()), originalKeywords))) {
                    documentFrequency.merge(token, 1, Integer::sum);
                }
            }
        }

        for (WebPage page : pages) {
            if (page.getHtml() == null) continue;
            
            // 使用實例方法 extract() 萃取正文
            String cleanText = textExtractor.extract(page.getHtml());
            List<String> tokens = tokenizer.tokenize(cleanText, originalKeywords); // 單字的原關鍵字（例如查詢詞「貓」）也保留

            Set<String> uniqueTokensInDoc = new HashSet<>(tokens);
            boolean containsOriginal = uniqueTokensInDoc.stream().anyMatch(originalKeywords::contains);

            // 計算 DF (Document Frequency)；有背景網頁時已在上面算好
            if (!useBackground) {
                for (String token : uniqueTokensInDoc) {
                    documentFrequency.put(token, documentFrequency.getOrDefault(token, 0) + 1);
                }
            }

            // 若網頁包含原關鍵字，建立詞與詞之間的共現鄰接表邊（Edge）
            if (containsOriginal) {
                for (int i = 0; i < tokens.size(); i++) {
                    String wordA = tokens.get(i);
                    if (wordA.length() <= 1 && !originalKeywords.contains(wordA)) continue;

                    adjacencyList.putIfAbsent(wordA, new HashMap<>());
                    Map<String, Integer> neighbors = adjacencyList.get(wordA);

                    // 以前後視窗 (Window size = 5) 內的詞建立共現關係邊
                    int windowEnd = Math.min(i + 5, tokens.size());
                    for (int j = i + 1; j < windowEnd; j++) {
                        String wordB = tokens.get(j);
                        if (!wordB.equals(wordA) && (wordB.length() > 1 || originalKeywords.contains(wordB))) {
                            neighbors.put(wordB, neighbors.getOrDefault(wordB, 0) + 1);
                            
                            // 無向圖雙向記錄
                            adjacencyList.putIfAbsent(wordB, new HashMap<>());
                            adjacencyList.get(wordB).put(wordA, adjacencyList.get(wordB).getOrDefault(wordA, 0) + 1);
                        }
                    }
                }
            }
        }

        // 2. 評估圖中的節點分數（結合原關鍵字的連線權重與 TF-IDF）
        Map<String, Double> wordScores = new HashMap<>();
        for (String origKw : originalKeywords) {
            Map<String, Integer> neighbors = adjacencyList.get(origKw);
            if (neighbors != null) {
                for (Map.Entry<String, Integer> edge : neighbors.entrySet()) {
                    String candidate = edge.getKey();
                    if (originalKeywords.contains(candidate)) continue;

                    int coFreq = edge.getValue();
                    int df = Math.max(1, documentFrequency.getOrDefault(candidate, 1));
                    double tfIdfWeight = coFreq * Math.log((double) totalDocs / df + 1.0);

                    wordScores.put(candidate, wordScores.getOrDefault(candidate, 0.0) + tfIdfWeight);
                }
            }
        }

        // 3. 使用 PriorityQueue 取出分數最高的前 K 個擴充詞
        PriorityQueue<CandidateWord> pq = new PriorityQueue<>(Comparator.comparingDouble(CandidateWord::getScore));
        for (Map.Entry<String, Double> entry : wordScores.entrySet()) {
            pq.offer(new CandidateWord(entry.getKey(), entry.getValue()));
            if (pq.size() > topK) {
                pq.poll();
            }
        }

        List<CandidateWord> result = new ArrayList<>();
        while (!pq.isEmpty()) {
            result.add(0, pq.poll()); // 轉為由大到小排序
        }
        return result;
    }
}