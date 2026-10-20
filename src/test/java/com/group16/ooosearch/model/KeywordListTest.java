package com.group16.ooosearch.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KeywordListTest {

    @TempDir
    Path tempDir;

    @Test
    void readsKeywordsAndSkipsCommentsAndBlankLines() throws IOException {
        Path file = write("# 註解\n\n假面騎士OOO 5\nKamen Rider OOO 2.5\n");

        KeywordList list = KeywordList.fromFile(file);

        assertEquals(2, list.size());
        assertEquals("假面騎士OOO", list.get(0).getName());
        assertEquals(5.0, list.get(0).getWeight());
        assertEquals("Kamen Rider OOO", list.get(1).getName());
        assertEquals(2.5, list.get(1).getWeight());
    }

    @Test
    void rejectsLineWithoutWeight() throws IOException {
        Path file = write("安卡\n");
        assertThrows(IllegalArgumentException.class, () -> KeywordList.fromFile(file));
    }

    @Test
    void rejectsNonNumericWeight() throws IOException {
        Path file = write("安卡 高\n");
        assertThrows(IllegalArgumentException.class, () -> KeywordList.fromFile(file));
    }

    @Test
    void projectKeywordsFileIsValid() throws IOException {
        KeywordList list = KeywordList.fromFile(Path.of("data", "keywords.txt"));
        assertEquals(6, list.size());
    }

    private Path write(String content) throws IOException {
        Path file = tempDir.resolve("keywords.txt");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
