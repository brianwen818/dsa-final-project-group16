package com.group16.ooosearch.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KeywordListTest {

    @TempDir
    Path tempDir;

    @Test
    void readsWeightNameAndAliasesAndSkipsComments() throws IOException {
        Path file = write("# 註解\n\n5.0 | 假面騎士OOO | オーズ, OOO，Kamen Rider OOO\n2.6 | 泉比奈 |\n0 | 安卡拉\n");

        KeywordList list = KeywordList.fromFile(file);

        assertEquals(3, list.size());
        assertEquals("假面騎士OOO", list.get(0).getName());
        assertEquals(5.0, list.get(0).getWeight());
        assertEquals(List.of("オーズ", "OOO", "Kamen Rider OOO"), list.get(0).getAliases());
        assertTrue(list.get(1).getAliases().isEmpty());
        assertEquals(0.0, list.get(2).getWeight());
    }

    @Test
    void rejectsLineWithoutName() throws IOException {
        Path file = write("3.0\n");
        assertThrows(IllegalArgumentException.class, () -> KeywordList.fromFile(file));
    }

    @Test
    void rejectsNonNumericWeight() throws IOException {
        Path file = write("高 | 安酷 | アンク\n");
        assertThrows(IllegalArgumentException.class, () -> KeywordList.fromFile(file));
    }

    @Test
    void projectKeywordsFileIsValid() throws IOException {
        KeywordList list = KeywordList.fromFile(Path.of("data", "keywords.txt"));
        assertFalse(list.isEmpty());
        for (Keyword keyword : list) {
            assertTrue(keyword.getWeight() >= 0 && keyword.getWeight() <= 5, keyword.toString());
        }
    }

    private Path write(String content) throws IOException {
        Path file = tempDir.resolve("keywords.txt");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
