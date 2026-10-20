package com.group16.ooosearch.fetch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PageCacheTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsEmptyWhenNotCached() throws IOException {
        PageCache cache = new PageCache(tempDir);
        assertTrue(cache.get("https://example.com/missing").isEmpty());
    }

    @Test
    void returnsStoredHtml() throws IOException {
        PageCache cache = new PageCache(tempDir);
        cache.put("https://example.com/ooo", "<html>假面騎士OOO</html>");

        assertEquals(Optional.of("<html>假面騎士OOO</html>"), cache.get("https://example.com/ooo"));
    }

    @Test
    void readsFromDiskInNewInstance() throws IOException {
        new PageCache(tempDir).put("https://example.com/ankh", "<html>安卡</html>");

        PageCache reopened = new PageCache(tempDir);

        assertEquals(Optional.of("<html>安卡</html>"), reopened.get("https://example.com/ankh"));
    }
}
