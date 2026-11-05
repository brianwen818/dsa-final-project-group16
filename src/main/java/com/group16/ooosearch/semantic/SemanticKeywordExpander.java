package com.group16.ooosearch.semantic;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.WebPage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Stage 4 的正式擴充器：從網頁（實際上由搜尋引擎傳入「相關段落」）中找出經常和原關鍵字一起出現的詞。
 *
 * <ol>
 *   <li>斷詞（jieba，關鍵字表的寫法已加入詞典）並建共現圖，候選分數見 {@link TfIdfAnalyzer}。</li>
 *   <li>剔除三類候選：已知寫法、已知寫法的碎片（例如「騎士」是「假面騎士OOO」的一部分）、
 *       其他作品或雜訊（{@code data/expansion-excludes.txt}）。</li>
 *   <li>分數達門檻的前 5 個詞加入，權重 = 1 + 1.5 × 相關度（相關度 = 分數 / 本輪最高分），最高 2.5，
 *       低於原關鍵字的最低權重 2.6。</li>
 * </ol>
 * 本類別沒有狀態，可以重複呼叫；最多擴充幾輪由搜尋引擎決定（{@link #MAX_ITERATIONS}）。
 */
public class SemanticKeywordExpander implements KeywordExpander {
    /** 最多擴充幾輪，避免越擴越偏題。 */
    public static final int MAX_ITERATIONS = 2;
    public static final int TOP_K = 5;
    public static final double RELEVANCE_THRESHOLD = 3.0;
    public static final double MAX_EXTRA_WEIGHT = 1.5;
    /** 只由漢字、假名、英數組成（「）」」「、「」這類標點不是詞）。 */
    private static final Pattern WORD = Pattern.compile("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}ー・·A-Za-z0-9]+");
    /** 純英數的候選詞至少要 4 個字（太短的多半是縮寫或雜訊）。 */
    private static final Pattern ASCII_WORD = Pattern.compile("[A-Za-z0-9]+");
    /** jieba 不會切日文，含平假名的候選詞多半是助詞碎片（例如「とストレスと」）；片假名詞（例如「シャム」）保留。 */
    private static final Pattern HIRAGANA = Pattern.compile("\\p{IsHiragana}");

    private final TfIdfAnalyzer analyzer;
    private final List<String> excludes;
    private List<Keyword> lastExpandedKeywords = List.of();
    private Map<String, Double> lastRelevance = Map.of();

    public SemanticKeywordExpander(Path stopwordsPath) {
        this(stopwordsPath, null, new KeywordList());
    }

    /**
     * @param excludesFile       其他作品名與雜訊詞（每行一個，# 開頭為註解）；null 表示不用
     * @param dictionaryKeywords 要加入 jieba 詞典的關鍵字（通常是 data/keywords.txt）
     */
    public SemanticKeywordExpander(Path stopwordsPath, Path excludesFile, KeywordList dictionaryKeywords) {
        this.analyzer = new TfIdfAnalyzer(new Tokenizer(stopwordsPath, dictionaryKeywords));
        this.excludes = loadExcludes(excludesFile);
    }

    @Override
    public KeywordList expand(List<WebPage> pages, KeywordList originalKeywords) {
        return expand(pages, originalKeywords, List.of());
    }

    /**
     * 同上；background 是計算 DF 用的背景網頁（搜尋引擎傳入所有爬到的網頁），讓泛用詞的分數被壓低。
     */
    public KeywordList expand(List<WebPage> pages, KeywordList originalKeywords, List<WebPage> background) {
        Set<String> knownForms = new HashSet<>();
        for (Keyword kw : originalKeywords) {
            knownForms.addAll(kw.getForms());
        }

        List<TfIdfAnalyzer.CandidateWord> candidates = analyzer.extractTopK(pages, knownForms, TOP_K * 4, background);
        List<TfIdfAnalyzer.CandidateWord> accepted = new ArrayList<>();
        for (TfIdfAnalyzer.CandidateWord candidate : candidates) {
            if (accepted.size() >= TOP_K) {
                break;
            }
            if (candidate.getScore() >= RELEVANCE_THRESHOLD && isUseful(candidate.getWord(), knownForms)) {
                accepted.add(candidate);
            }
        }

        KeywordList expandedList = new KeywordList(originalKeywords.toList());
        List<Keyword> added = new ArrayList<>();
        Map<String, Double> relevance = new LinkedHashMap<>();
        double maxScore = accepted.isEmpty() ? 1 : accepted.get(0).getScore();
        for (TfIdfAnalyzer.CandidateWord candidate : accepted) {
            double rel = candidate.getScore() / maxScore;
            double weight = Math.round((1 + MAX_EXTRA_WEIGHT * rel) * 10) / 10.0;
            Keyword keyword = new Keyword(candidate.getWord(), weight, List.of());
            expandedList.add(keyword);
            added.add(keyword);
            relevance.put(candidate.getWord(), rel);
        }
        lastExpandedKeywords = List.copyOf(added);
        lastRelevance = Map.copyOf(relevance);
        return expandedList;
    }

    /** 最近一次擴充出的新關鍵字（不含原關鍵字）。 */
    public List<Keyword> getLastExpandedKeywords() {
        return lastExpandedKeywords;
    }

    /** 最近一次擴充詞的相關度（0–1）。 */
    public Map<String, Double> getLastRelevance() {
        return lastRelevance;
    }

    /** 是一個詞，而且不是已知寫法、不是已知寫法的碎片、也不是其他作品或雜訊。 */
    boolean isUseful(String word, Set<String> knownForms) {
        if (!WORD.matcher(word).matches() || (ASCII_WORD.matcher(word).matches() && word.length() < 4)
                || HIRAGANA.matcher(word).find()) {
            return false;
        }
        String w = word.toLowerCase(Locale.ROOT);
        for (String form : knownForms) {
            String f = form.toLowerCase(Locale.ROOT);
            if (f.equals(w) || f.contains(w)) {
                return false;
            }
        }
        for (String exclude : excludes) {
            boolean hit = exclude.length() <= 2 ? w.equals(exclude) : w.contains(exclude);
            if (hit) {
                return false;
            }
        }
        return true;
    }

    private static List<String> loadExcludes(Path file) {
        List<String> result = new ArrayList<>();
        if (file == null || !Files.exists(file)) {
            return result;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String t = line.strip();
                if (!t.isEmpty() && !t.startsWith("#")) {
                    result.add(t.toLowerCase(Locale.ROOT));
                }
            }
        } catch (IOException e) {
            System.err.println("無法載入擴充排除詞: " + e.getMessage());
        }
        return result;
    }
}
