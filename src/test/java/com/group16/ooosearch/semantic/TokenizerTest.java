package com.group16.ooosearch.semantic;

import org.junit.jupiter.api.Test;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerTest {
    private static final Path STOPWORDS = Path.of("data", "stopwords.txt");

    @Test
    void testTokenizeWithStopwords() {
        Tokenizer tokenizer = new Tokenizer(STOPWORDS);

        String text = "假面騎士OOO的核心硬幣是非常重要的道具，安酷與映司的冒險。";
        List<String> tokens = tokenizer.tokenize(text);

        // 驗證停用詞「的」、「是」、「與」被成功過濾
        assertFalse(tokens.contains("的"));
        assertFalse(tokens.contains("是"));
        assertFalse(tokens.contains("與"));

        // 驗證核心詞彙被保留
        assertTrue(tokens.contains("核心") || tokens.contains("硬幣") || tokens.contains("安酷") || tokens.contains("映司"));
    }

    @Test
    void keywordFormsAreLoadedIntoTheDictionary() {
        KeywordList keywords = new KeywordList(List.of(
                new Keyword("假面騎士OOO", 5, List.of("OOO")),
                new Keyword("鴻上基金會", 3, List.of()),
                new Keyword("獅虎豹組合", 2.9, List.of("貓系聯組"))));
        Tokenizer tokenizer = new Tokenizer(STOPWORDS, keywords);

        List<String> tokens = tokenizer.tokenize("假面騎士OOO的獅虎豹組合是貓系聯組，鴻上基金會也登場。");

        assertTrue(tokens.contains("假面騎士"), tokens.toString()); // 不再被切成「假面」「騎士」
        assertFalse(tokens.contains("騎士"), tokens.toString());
        assertTrue(tokens.contains("獅虎豹組合"), tokens.toString());
        assertTrue(tokens.contains("貓系聯組"), tokens.toString());
        assertTrue(tokens.contains("鴻上基金會"), tokens.toString());
    }

    @Test
    void singleCharacterWordsCanBeKept() {
        Tokenizer tokenizer = new Tokenizer(STOPWORDS);

        assertFalse(tokenizer.tokenize("一隻貓").contains("貓"));
        assertTrue(tokenizer.tokenize("一隻貓", Set.of("貓")).contains("貓"));
    }
}
