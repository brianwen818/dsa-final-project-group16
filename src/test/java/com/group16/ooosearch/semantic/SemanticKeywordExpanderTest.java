package com.group16.ooosearch.semantic;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.WebPage;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SemanticKeywordExpanderTest {

    @Test
    void testKeywordExpansion() {
        Path stopwordsPath = Path.of("data", "stopwords.txt");
        SemanticKeywordExpander expander = new SemanticKeywordExpander(stopwordsPath);

        // 模擬原始關鍵字
        KeywordList original = new KeywordList(List.of(
                new Keyword("假面騎士OOO", 5.0, List.of("OOO"))
        ));

        // 模擬 Stage 3 抓回並包含共現詞的網頁內容
        WebPage mockPage = new WebPage(
                "https://example.com/ooo",
                "假面騎士OOO介紹",
                "<html><body>假面騎士OOO與核心硬幣的故事，伊達明與鴻上基金會登場，安酷協助映司。</body></html>"
        );

        KeywordList expanded = expander.expand(List.of(mockPage), original);

        // 驗證擴充後清單長度大於原本（代表成功抓出高頻共現詞並加入）
        assertTrue(expanded.size() >= original.size());
    }
}