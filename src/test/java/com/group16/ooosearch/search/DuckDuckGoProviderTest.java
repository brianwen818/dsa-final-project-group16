package com.group16.ooosearch.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.group16.ooosearch.fetch.PageCache;
import com.group16.ooosearch.fetch.PageFetcher;

class DuckDuckGoProviderTest {
    @Test
    void decodesRedirectLinksAndRespectsLimit(@TempDir Path dir) throws IOException {
        PageCache cache = new PageCache(dir);
        cache.put("https://html.duckduckgo.com/html/?q=OOO", """
                <a class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fa.test%2Fooo&amp;rut=x">a</a>
                <a class="result__a" href="https://b.test/ooo">b</a>
                <a class="result__a" href="https://c.test/ooo">c</a>
                """);

        List<String> urls = new DuckDuckGoProvider(new PageFetcher(cache)).search("OOO", 2);

        assertEquals(List.of("https://a.test/ooo", "https://b.test/ooo"), urls);
    }
}
