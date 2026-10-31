package com.group16.ooosearch.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class SearchAggregatorTest {
    @Test
    void mergesProvidersInOrderAndRemovesSamePageWrittenDifferently() {
        SearchProvider first = (query, limit) -> List.of("https://zh.wikipedia.org/wiki/%e5%ae%89", "https://a.test/1");
        SearchProvider second = (query, limit) -> List.of("https://zh.wikipedia.org/zh-tw/%E5%AE%89", "https://b.test/");

        List<String> urls = new SearchAggregator(List.of(first, second)).search("q", 5);

        assertEquals(List.of("https://zh.wikipedia.org/wiki/%E5%AE%89", "https://a.test/1", "https://b.test/"), urls);
    }

    @Test
    void failingProviderDoesNotStopOthers() {
        SearchProvider broken = (query, limit) -> {
            throw new IllegalStateException("down");
        };
        SearchProvider working = (query, limit) -> List.of("https://a.test/1");

        assertEquals(List.of("https://a.test/1"), new SearchAggregator(List.of(broken, working)).search("q", 5));
    }
}
