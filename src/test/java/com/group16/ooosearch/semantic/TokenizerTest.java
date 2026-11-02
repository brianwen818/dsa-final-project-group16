package com.group16.ooosearch.semantic;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerTest {

    @Test
    void testTokenizeWithStopwords() {
        Path stopwordsPath = Path.of("data", "stopwords.txt");
        Tokenizer tokenizer = new Tokenizer(stopwordsPath);

        String text = "假面騎士OOO的核心硬幣是非常重要的道具，安酷與映司的冒險。";
        List<String> tokens = tokenizer.tokenize(text);

        // 驗證停用詞「的」、「是」、「與」被成功過濾
        assertFalse(tokens.contains("的"));
        assertFalse(tokens.contains("是"));
        assertFalse(tokens.contains("與"));
        
        // 驗證核心詞彙被保留
        assertTrue(tokens.contains("核心") || tokens.contains("硬幣") || tokens.contains("安酷") || tokens.contains("映司"));
    }
}