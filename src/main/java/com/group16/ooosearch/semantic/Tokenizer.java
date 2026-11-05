package com.group16.ooosearch.semantic;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.WordDictionary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 中文斷詞（jieba-analysis）並過濾停用詞。
 *
 * <p>jieba 的內建詞典沒有特攝專有名詞，「假面騎士」會被切成「假面」「騎士」，擴充詞就會變成這些碎片。
 * 所以建立時可以傳入關鍵字表：每個寫法中的連續漢字／假名片段（例如「假面騎士OOO」中的「假面騎士」）
 * 都會寫進 jieba 的使用者詞典。
 */
public class Tokenizer {
    /** 一定要當成一個詞的通用詞。 */
    private static final List<String> BASE_WORDS = List.of("假面騎士", "假面骑士", "仮面ライダー");
    /** 漢字與假名的連續片段；jieba 會把漢字與英數字分開處理，所以詞典只能放這種片段。 */
    private static final Pattern CJK_RUN = Pattern.compile("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}ー]{2,}");
    private static final Set<String> LOADED_WORDS = new HashSet<>();

    private final JiebaSegmenter segmenter;
    private final Set<String> stopwords;

    public Tokenizer(Path stopwordsPath) {
        this(stopwordsPath, new KeywordList());
    }

    public Tokenizer(Path stopwordsPath, KeywordList dictionaryKeywords) {
        loadKeywordDictionary(dictionaryKeywords);
        this.segmenter = new JiebaSegmenter();
        this.stopwords = loadStopwords(stopwordsPath);
    }

    /** 載入現成的 jieba 使用者詞典檔（每行「詞 頻率 詞性」）。 */
    public void loadCustomDictionary(Path customDictPath) {
        if (Files.exists(customDictPath)) {
            WordDictionary.getInstance().loadUserDict(customDictPath);
        }
    }

    /** 把關鍵字的所有寫法（取漢字／假名片段）加入 jieba 詞典；同一個詞只載入一次。 */
    private static synchronized void loadKeywordDictionary(KeywordList keywords) {
        Set<String> words = new LinkedHashSet<>(BASE_WORDS);
        for (Keyword keyword : keywords) {
            for (String form : keyword.getForms()) {
                Matcher m = CJK_RUN.matcher(form);
                while (m.find()) {
                    words.add(m.group());
                }
            }
        }
        words.removeAll(LOADED_WORDS);
        if (words.isEmpty()) {
            return;
        }
        try {
            Path dict = Files.createTempFile("ooo-jieba-", ".dict");
            StringBuilder lines = new StringBuilder();
            for (String word : words) {
                lines.append(word).append(" 3000 n\n");
            }
            Files.writeString(dict, lines, StandardCharsets.UTF_8);
            WordDictionary.getInstance().loadUserDict(dict);
            Files.deleteIfExists(dict);
            LOADED_WORDS.addAll(words);
        } catch (IOException e) {
            System.err.println("無法建立 jieba 自訂詞典: " + e.getMessage());
        }
    }

    private Set<String> loadStopwords(Path path) {
        Set<String> words = new HashSet<>();
        if (path != null && Files.exists(path)) {
            try {
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
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

    /** 斷詞並過濾停用詞、單字與純數字。 */
    public List<String> tokenize(String text) {
        return tokenize(text, Set.of());
    }

    /** 同上，但 keep 中的詞即使只有一個字也保留（例如查詢詞「貓」）。 */
    public List<String> tokenize(String text, Set<String> keep) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<String> rawTokens = segmenter.sentenceProcess(text);
        return rawTokens.stream()
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .filter(token -> token.length() > 1 || keep.contains(token)) // 過濾單字元噪音
                .filter(token -> !stopwords.contains(token)) // 過濾停用詞
                .filter(token -> !token.matches("^\\d+$")) // 過濾純數字
                .collect(Collectors.toList());
    }
}
