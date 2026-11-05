package com.group16.ooosearch.semantic;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.WebPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SemanticKeywordExpanderTest {
    private static final Path STOPWORDS = Path.of("data", "stopwords.txt");

    private final KeywordList original = new KeywordList(List.of(
            new Keyword("假面騎士OOO", 5.0, List.of("OOO")),
            new Keyword("核心硬幣", 4.5, List.of())));

    private SemanticKeywordExpander expander(Path dir) throws IOException {
        Path excludes = dir.resolve("excludes.txt");
        Files.writeString(excludes, "# 測試\nFourze\n騎士\n", StandardCharsets.UTF_8);
        // 和搜尋引擎一樣把原關鍵字放進詞典；「紫色硬幣」也放進詞典（但不是原關鍵字），確保斷詞穩定
        List<Keyword> words = new java.util.ArrayList<>(original.toList());
        words.add(new Keyword("紫色硬幣", 1.0));
        words.add(new Keyword("假面騎士Fourze", 1.0));
        KeywordList dictionary = new KeywordList(words);
        return new SemanticKeywordExpander(STOPWORDS, excludes, dictionary);
    }

    private static List<WebPage> pages() {
        String body = "<p>假面騎士OOO 使用 紫色硬幣 ，核心硬幣 之中 紫色硬幣 最 危險 。假面騎士Fourze 客串 。</p>";
        return List.of(new WebPage("https://a.test/1", "", body), new WebPage("https://a.test/2", "", body),
                new WebPage("https://a.test/3", "", body));
    }

    @Test
    void expandsWithRelevanceWeightedTermsAndIsRepeatable(@TempDir Path dir) throws IOException {
        SemanticKeywordExpander expander = expander(dir);

        KeywordList first = expander.expand(pages(), original);
        List<Keyword> added = expander.getLastExpandedKeywords();

        assertTrue(added.stream().anyMatch(k -> k.getName().equals("紫色硬幣")), added.toString());
        for (Keyword k : added) {
            assertTrue(k.getWeight() >= 1.0 && k.getWeight() <= 2.5, k.toString());
        }
        assertEquals(2.5, added.get(0).getWeight()); // 第 1 名相關度為 1
        assertEquals(original.size() + added.size(), first.size());

        // 沒有狀態：再呼叫一次結果相同（舊版第二次會直接回傳原關鍵字）
        expander.expand(pages(), original);
        assertEquals(added.toString(), expander.getLastExpandedKeywords().toString());
    }

    @Test
    void fragmentsOtherWorksAndKnownFormsAreRejected(@TempDir Path dir) throws IOException {
        SemanticKeywordExpander expander = expander(dir);
        Set<String> known = Set.of("假面騎士OOO", "OOO", "核心硬幣");

        assertFalse(expander.isUseful("騎士", known));        // 排除表（也是碎片）
        assertFalse(expander.isUseful("硬幣", known));        // 「核心硬幣」的碎片
        assertFalse(expander.isUseful("核心硬幣", known));    // 已知寫法
        assertFalse(expander.isUseful("假面騎士Fourze", known)); // 其他作品
        assertTrue(expander.isUseful("紫色硬幣", known));
        assertFalse(expander.isUseful("）」", known));         // 標點
        assertFalse(expander.isUseful("とストレスと", known)); // 日文助詞碎片
        assertTrue(expander.isUseful("シャム", known));        // 片假名詞保留
        assertFalse(expander.isUseful("Ta", known));           // 太短的英數
    }
}
